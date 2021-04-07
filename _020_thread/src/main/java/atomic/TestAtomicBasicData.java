package atomic;

import java.util.concurrent.atomic.AtomicInteger;

public class TestAtomicBasicData {
	public static void main(String[] args) throws InterruptedException {
		AtomicInteger atomicInteger = new AtomicInteger(8);
		System.out.println("初始化:" + atomicInteger);//8
		atomicInteger.compareAndSet(8, 10);
		System.out.println("CAS后:" + atomicInteger);//10
		System.out.println(atomicInteger.getAndIncrement());//自增1，返回自增前的值10
		System.out.println("自增后：" + atomicInteger);//11
		System.out.println(atomicInteger.getAndDecrement());//11
		System.out.println("自减后：" + atomicInteger);//10
	}
}