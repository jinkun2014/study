package lock;

import java.util.concurrent.locks.LockSupport;

public class LockParkInterruptDemo {

	public static void main(String[] args) throws InterruptedException {
		Thread t1 = new Thread(() -> {
			int i = 0;
			while (true) {
				if (i == 0) {
					LockSupport.park();
					//获取中断标记，但是不复位
					System.out.println(Thread.currentThread().isInterrupted());
					// 获取并复位中断标记
					// System.out.println(Thread.interrupted());
					LockSupport.park();
					System.out.println("如果走到这里就说明park不生效了");
				}
				i++;
				if (i == Integer.MAX_VALUE) {
					break;
				}
			}
		});
		t1.start();
		Thread.sleep(1000);//确保t1被park()之后再中断
		System.out.println("start interrupt...");
		t1.interrupt();
		System.out.println("end");
	}
}