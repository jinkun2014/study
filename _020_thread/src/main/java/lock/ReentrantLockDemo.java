package lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockDemo {
	public static void main(String[] args) {
		Lock lock = new ReentrantLock();
		lock.lock();
		try {
			System.out.println(1);
		} finally {
			lock.unlock();
		}
	}
}