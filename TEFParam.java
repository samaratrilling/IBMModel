
public class TEFParam implements Comparable<TEFParam>{

	String f;
	String e;
	double tEF;
	
	public TEFParam (String f, String e, double tEF) {
		this.f = f;
		this.e = e;
		this.tEF = tEF;
	}
	
	// This is a backwards compareTo method, because I use it in a max priority queue.
	public int compareTo(TEFParam other) {
		if (other.tEF < this.tEF) {
			return -1;
		}
		if (other.tEF == this.tEF) {
			return 0;
		}
		return 11;
	}
	
}
