/*

Demonstrate snapshot+restore capabilities with basic multithreading and inflated monitor restoration.
This application forces inflation of an object monitor and snapshots while the inflated monitor is acquired.
A second Java thread is also running at the time of snapshot to demonstrate multithreading.

Note deflation policy option (-Xthr:deflationPolicy=never) is used to ensure object monitors
have no chance of deflating before snapshot.
Attach API thread is also disabled (-Dcom.ibm.tools.attach.enable=no) since it cannot be restored at this time.

turn off ASLR:
setarch `uname -m` -R /bin/bash

create snapshot:
`./java -Xint -Xmx64m -Xms8m -Xgcpolicy:optthruput -Xshare:off -Xsnapshot:file=image,trigger=HelloMultiMonitor.snapshot -Xthr:deflationPolicy=never -Dcom.ibm.tools.attach.enable=no -cp ~/. HelloMultiMonitor`

restore snapshot:
`./java -Xint -Xmx64m -Xms9m -Xgcpolicy:optthruput -Xshare:off -Xrestore:file=image -Xthr:deflationPolicy=never -Dcom.ibm.tools.attach.enable=no -cp ~/. HelloMultiMonitor`

*/

public class HelloMultiMonitor {
	static double d = Math.random();

	static void snapshot() {
	}

	public static Object monitor = new Object();
	public static Object waitForStart = new Object();

	public static void main(String args[]) throws Throwable {
		/* initialize vm */
		System.out.print("Do bunch of init .");
		Thread.sleep(1500);
		System.out.print(" .");
		Thread.sleep(1500);
		System.out.print(" .");
		Thread.sleep(1500);
		System.out.print(" .");
		Thread.sleep(1500);
		System.out.println("\n finished init");

		/**
		 * Force object monitor to inflate. A flat object monitor can support a single
		 * thread waiting to acquire the monitor. The monitor must inflate to support 2.
		 */
		InflationThread iThread1 = new InflationThread();
		InflationThread iThread2 = new InflationThread();
		iThread1.start();
		iThread2.start();

		/*
		 * wait for all threads to be done and monitor to be inflated before continuing
		 * to snapshot.
		 */
		iThread1.join();
		iThread2.join();

		/*
		 * Snapshot while additional SnapshotThread is running and inflated object
		 * monitor is acquired.
		 */
		synchronized (monitor) {
			System.out.println("start snapshot\n");
			SnapshotThread snapshotThread = new SnapshotThread();
			snapshotThread.start();

			/* don't snapshot until SnapshotThread notifies it is running. */
			synchronized (waitForStart) {
				waitForStart.wait();
			}

			snapshot();// <--- snapshot here
		}

		System.out.println("Starting receiving requests");
	}
}

/* Acquire object monitor to force contention. */
class InflationThread extends Thread {
	public void run() {
		try {
			synchronized (HelloMultiMonitor.monitor) {
				Thread.sleep(500);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}

class SnapshotThread extends Thread {
	public void run() {
		try {
			for (int i = 0; i < 5; i++) {
				System.out.println("Snapshot second thread start " + i + "\n");
				Thread.sleep(1500);
			}
			/* notify main that thread has started. */
			synchronized (HelloMultiMonitor.waitForStart) {
				HelloMultiMonitor.waitForStart.notify();
			}

			Thread.sleep(1500);

			/*
			 * force a TLH refresh to prevent random seg faults in non-main thread
			 */
			System.out.println("Snapshot second thread force TLH refresh");
			for (int i = 0; i < 100; i++) {
				Object newobject = new Object();
			}

			System.out.println("Snapshot second thread continue ");
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
