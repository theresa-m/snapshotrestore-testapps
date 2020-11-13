/*
turn off ASLR:
setarch `uname -m` -R /bin/bash

create snapshot:
`./java -Xint -Xmx16m -Xms8m -Xgcpolicy:optthruput -Xshare:off -Xsnapshot:file=image,trigger=Hello.snapshot -cp ~/. Hello`

restore snapshot:
`./java -Xint -Xmx16m -Xms9m -Xgcpolicy:optthruput -Xshare:off -Xrestore:file=image -cp ~/. Hello`

*/

public class Hello {
	static double d  = Math.random();

	static void snapshot() {}

	public static void main(String args[]) throws Throwable  {
		System.out.print("Do bunch of init .");
		Thread.sleep(1500);
		System.out.print(" .");
                Thread.sleep(1500);
		System.out.print(" .");
                Thread.sleep(1500);
		System.out.print(" .");
                Thread.sleep(1500);
		System.out.println("\n finished init");
		snapshot();//<--- snapshot here
		System.out.println("Starting receiving requests");
	}
}
