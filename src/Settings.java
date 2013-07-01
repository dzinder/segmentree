
import java.util.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.io.*;

public class Settings
{
	static Map<Class<?>, Map<String, Field>> settingFields
	= new HashMap<Class<?>, Map<String,Field>>();

	public static void printSettings(Object obj, OutputStream s)
	{
		if(obj instanceof Class)
		{
			printSettings(obj, (Class<?>)obj, s);
		}
		else
		{
			Class<?> aClass = obj.getClass();

			while(aClass != Object.class)
			{
				printSettings(obj, aClass, s);
				aClass = aClass.getSuperclass();
			}
		}
	}

	public static void printSettingsDescription(Object obj, OutputStream s) {
		if(obj instanceof Class)
		{
			printSettingsDescription(obj, (Class<?>)obj, s);
		}
		else
		{
			Class<?> aClass = obj.getClass();

			while(aClass != Object.class)
			{
				printSettingsDescription(obj, aClass, s);
				aClass = aClass.getSuperclass();
			}
		}
	}


	public static void printSettingsDescription(Object obj, Class<?> objClass, OutputStream s)
	{
		PrintStream ps = new PrintStream(s);

		Field[] fields = objClass.getDeclaredFields();
		for(Field field : fields)
		{
			if(field.getAnnotation(Setting.class) != null)
			{
				field.setAccessible(true);
				try
				{
					ps.println(field.getName() + " - " + ((Setting) field.getAnnotations()[0]).description());
				}
				catch (Exception e) { }
				field.setAccessible(false);
			}
		}
	}

	public static void printSettings(Object obj, Class<?> objClass, OutputStream s)
	{
		PrintStream ps = new PrintStream(s);

		Field[] fields = objClass.getDeclaredFields();
		for(Field field : fields)
		{
			if(field.getAnnotation(Setting.class) != null)
			{
				field.setAccessible(true);
				try
				{											
					if (field.getType() == double[].class) {											
						ps.println(field.getName() + "," + Arrays.toString((double[]) field.get(obj)));
					} 
					else if  (field.getType() == String[].class) {
						ps.println(field.getName() + "," + Arrays.toString((String[]) field.get(obj)));
					}
					else if  (field.getType() == int[].class) {
						ps.println(field.getName() + "," + Arrays.toString((int[]) field.get(obj)));
					}
					else {						
						ps.println(field.getName() + "," + field.get(obj));
					}
				}
				catch (Exception e) { }
				field.setAccessible(false);
			}
		}
	}

	private HashMap<String, String> hm;

	public Settings()
	{
		hm = new HashMap<String, String>();
	}

	public Settings(String[] args)
	{
		this();
		loadArgs(args);
	}

	public Settings(File settings) throws IOException
	{
		this();
		loadSettingsFile(settings);
	}

	public void loadArgs(String[] args)
	{
		for(String arg : args)
		{
			loadArg(arg);
		}
	}

	public void loadArg(String arg)
	{
		String[] pieces = arg.split("=");
		if(pieces.length == 2)
		{
			String setting = pieces[0].trim();
			String value = pieces[1].trim();

			hm.put(setting.toLowerCase(), value);
		}
	}

	public void loadSettingsFile(String settingsFilename) throws IOException
	{
		loadSettingsFile(new File(settingsFilename));
	}

	public void loadSettingsFile(File settingsFile) throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(settingsFile)));

		while(reader.ready())
		{
			String line = reader.readLine().trim();
			if(!line.startsWith("#"))
			{
				loadArg(line);
			}
		}
		reader.close();
	}

	public String get(String setting)
	{
		return hm.get(setting.toLowerCase());
	}

	public void put(String setting, String value)
	{
		hm.put(setting.toLowerCase(), value);
	}

	public void apply(Object obj)
	{
		if(obj instanceof Class)
		{
			apply(obj, (Class<?>)obj);
		}
		else
		{
			Class<?> aClass = obj.getClass();

			while(aClass != Object.class)
			{
				apply(obj, aClass);
				aClass = aClass.getSuperclass();
			}
		}
	}

	public void apply(Object obj, String setting)
	{
		if(obj instanceof Class)
		{
			apply(obj, (Class<?>)obj, setting);
		}
		else
		{
			Class<?> aClass = obj.getClass();

			while(aClass != Object.class)
			{
				apply(obj, aClass, setting);
				aClass = aClass.getSuperclass();
			}
		}
	}

	public void apply(Object obj, Class<?> objClass, String setting)
	{
		Field settingField = getSettingField(objClass, setting);
		if(settingField != null)
		{
			Object value = hm.get(setting);

			if(value != null)
			{
				Class<?> fieldClass = settingField.getType();
				if(fieldClass != String.class && value instanceof String)
				{
					String valueStr = (String)value;

					if(valueStr.equals("null"))
					{
						value = null;
					}
					else if(fieldClass == int.class || fieldClass == Integer.class)
					{
						value = Integer.parseInt(valueStr);
					}
					else if(fieldClass == byte.class || fieldClass == Integer.class)
					{
						value = Byte.parseByte(valueStr);
					}
					else if(fieldClass == long.class || fieldClass == Long.class)
					{
						value = Long.parseLong(valueStr);
					}
					else if(fieldClass == short.class || fieldClass == Short.class)
					{
						value = Short.parseShort(valueStr);
					}
					else if(fieldClass == double.class || fieldClass == Double.class)
					{
						value = Double.parseDouble(valueStr);
					}
					else if(fieldClass == float.class || fieldClass == Float.class)
					{
						value = Float.parseFloat(valueStr);
					}
					else if(fieldClass == BigDecimal.class)
					{
						value = new BigDecimal(valueStr);
					}
					else if(fieldClass == boolean.class || fieldClass == Boolean.class)
					{
						value = Boolean.parseBoolean(valueStr);
					}
					else if(fieldClass.isEnum())
					{
						value = Enum.valueOf((Class<Enum>)fieldClass, valueStr);
					}
					else if(fieldClass == double[].class) {
						value = toDoubleArray(valueStr);						
					}
					else if(fieldClass == String[].class) {
						value = toStringArray(valueStr);						
					}
					else if(fieldClass == int[].class) {
						value = toIntArray(valueStr);						
					}
				}
			}

			try
			{
				settingField.setAccessible(true);
				settingField.set(obj, value);
				settingField.setAccessible(false);
			}
			catch(Exception e)
			{
				System.err.println("Could not set setting " + setting + "," + value);
			}
		}

	}

	@SuppressWarnings("rawtypes")
	public void apply(Object obj, Class objClass)
	{
		for(String setting : hm.keySet())
		{
			apply(obj, objClass, setting);
		}
	}

	private static Field getSettingField(Class<?> objClass, String setting)
	{
		Map<String, Field> classSettingFields = settingFields.get(objClass);
		if(classSettingFields == null)
		{
			classSettingFields = getClassSettingFields(objClass);
			settingFields.put(objClass, classSettingFields); 
		}
		return classSettingFields.get(setting.toLowerCase());
	}

	private static Map<String, Field> getClassSettingFields(Class<?> aClass)
	{
		Map<String, Field> classSettingFields = new HashMap<String, Field>();

		Field[] fields = aClass.getDeclaredFields();
		for(Field field : fields)
		{
			Setting settingObj = field.getAnnotation(Setting.class);
			if(settingObj != null)
			{
				String lowerName = field.getName().toLowerCase();

				if(settingObj.allowFieldName())
					classSettingFields.put(lowerName, field);
				if(!settingObj.shortName().equals(""))
				{
					String lowerShortName = settingObj.shortName().toLowerCase(); 
					classSettingFields.put(lowerShortName, field);
				}
			}
		}

		return classSettingFields;
	}

	public String toString()
	{
		return hm.toString();
	}

	static double[] toDoubleArray(String str) {

		try {
			String[] stringArray = str.split("[,}{ ]");

			int numNumbers = 0;
			for (int i=0; i<stringArray.length; i++) {
				if (!stringArray[i].isEmpty()) {
					numNumbers+=1;
				}
			}

			double[] doubleArray = new double[numNumbers];

			int currentIndex = 0;
			for (int i=0; i<stringArray.length; i++) {
				if (!stringArray[i].isEmpty()) {
					doubleArray[currentIndex]=Double.valueOf(stringArray[i]);
					currentIndex+=1;
				}
			}

			return doubleArray;
		}
		catch(Exception e) {
			System.err.println("Failed to parse double[]\n");
			return null;
		}

	}

	static String[] toStringArray(String str) {

		try {
			String[] stringArray = str.split("[,}{ ]");

			int numWords = 0;
			for (int i=0; i<stringArray.length; i++) {
				if (!stringArray[i].isEmpty()) {
					numWords+=1;
				}
			}

			String[] wordArray = new String[numWords];

			int currentIndex = 0;
			for (int i=0; i<stringArray.length; i++) {
				if (!stringArray[i].isEmpty()) {
					wordArray[currentIndex]=stringArray[i];
					currentIndex+=1;
				}
			}

			return wordArray;
		}
		catch(Exception e) {
			System.err.println("Failed to parse String[]\n");
			return null;
		}

	}

	static int[] toIntArray(String str) {

		try {
			String[] stringArray = str.split("[,}{ ]");

			int numInts = 0;
			for (int i=0; i<stringArray.length; i++) {
				if (!stringArray[i].isEmpty()) {
					numInts+=1;
				}
			}

			int[] intArray = new int[numInts];

			int currentIndex = 0;
			for (int i=0; i<stringArray.length; i++) {
				if (!stringArray[i].isEmpty()) {
					intArray[currentIndex]=Integer.valueOf(stringArray[i]);
					currentIndex+=1;
				}
			}

			return intArray;
		}
		catch(Exception e) {
			System.err.println("Failed to parse int[]\n");
			return null;
		}

	}
}
