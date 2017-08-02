
public class Pair<TKEY, TVALUE> {
	
	TKEY key;
	TVALUE value;
	
	Pair (TKEY key, TVALUE value) {
		this.key=key;
		this.value=value;
	}

	
	public int hashCode() {
		return key.hashCode();
	}


	public TVALUE getValue() {
		return value;
	}


	public TKEY getKey() {
		return key;
	}
}
