package org.balinhui.fpaplayer.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class BufferTest {

    private Buffer buffer;

    @BeforeEach
    void setUp() throws Exception {
        // 获取单例实例
        buffer = Buffer.getInstance();
        // 清空缓冲区，确保每个测试开始时状态干净
        buffer.clear();

        // 通过反射清理内部容器，避免之前测试留下的数据影响
        clearInternalContainers();
    }

    @AfterEach
    void tearDown() {
        buffer.clear();
    }

    /**
     * 通过反射重置 shortData 和 floatData 容器
     */
    private void clearInternalContainers() throws Exception {
        Field shortDataField = Buffer.class.getDeclaredField("shortData");
        shortDataField.setAccessible(true);
        List<short[]> shortData = (List<short[]>) shortDataField.get(null);
        shortData.clear();

        Field floatDataField = Buffer.class.getDeclaredField("floatData");
        floatDataField.setAccessible(true);
        List<float[]> floatData = (List<float[]>) floatDataField.get(null);
        floatData.clear();
    }

    // ======================= 基础功能测试 =======================

    @Test
    void testPutAndTakeFloatData() {
        int samplesNumber = 100;
        int oldSamplesNumber = 50;
        int size = 256;

        float[] array = buffer.putFloatData(samplesNumber, oldSamplesNumber, size);

        assertNotNull(array);
        assertEquals(size, array.length);

        Buffer.Data data = buffer.takeData();
        assertNotNull(data);
        assertEquals(samplesNumber, data.samplesNumber());
        assertEquals(oldSamplesNumber, data.oldSamplesNumber());
        assertEquals(Buffer.DataType.FLOAT, data.currentDataType());

        float[] retrievedArray = data.getFloatArray();
        assertSame(array, retrievedArray);
    }

    @Test
    void testPutAndTakeShortData() {
        int samplesNumber = 200;
        int oldSamplesNumber = 100;
        int size = 512;

        short[] array = buffer.putShortData(samplesNumber, oldSamplesNumber, size);

        assertNotNull(array);
        assertEquals(size, array.length);

        Buffer.Data data = buffer.takeData();
        assertEquals(Buffer.DataType.SHORT, data.currentDataType());

        short[] retrievedArray = data.getShortArray();
        assertSame(array, retrievedArray);
    }

    @Test
    void testBufferSizeLimit() throws InterruptedException {
        // 缓冲区容量为 50
        for (int i = 0; i < 50; i++) {
            buffer.putFloatData(i, 0, 10);
        }

        // 第 51 次 put 会阻塞，这里用超时检测
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<float[]> future = executor.submit(() -> buffer.putFloatData(100, 0, 10));

        try {
            future.get(100, TimeUnit.MILLISECONDS);
            fail("Expected blocking on full queue");
        } catch (TimeoutException | ExecutionException e) {
            // 预期超时，队列已满
            assertTrue(true);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void testTakeWhenEmptyBlocks() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Buffer.Data> future = executor.submit(() -> buffer.takeData());

        try {
            future.get(100, TimeUnit.MILLISECONDS);
            fail("Expected blocking on empty queue");
        } catch (TimeoutException e) {
            // 预期阻塞
            assertTrue(true);
        } finally {
            executor.shutdownNow();
        }
    }

    // ======================= 数组扩容测试 =======================

    @Test
    void testFloatArrayAutoResize() {
        int initialSize = 10;
        int largerSize = 100;

        float[] firstArray = buffer.putFloatData(0, 0, initialSize);
        assertEquals(initialSize, firstArray.length);

        // 放入更大 size 的数据
        float[] largerArray = buffer.putFloatData(0, 0, largerSize);
        assertNotSame(firstArray, largerArray);
        assertEquals(largerSize, largerArray.length);
    }

    @Test
    void testShortArrayAutoResize() {
        int initialSize = 20;
        int largerSize = 200;

        short[] firstArray = buffer.putShortData(0, 0, initialSize);
        assertEquals(initialSize, firstArray.length);

        short[] largerArray = buffer.putShortData(0, 0, largerSize);
        assertNotSame(firstArray, largerArray);
        assertEquals(largerSize, largerArray.length);
    }

    // ======================= 循环覆盖测试 =======================

    @Test
    void testCircularBufferFloat() throws InterruptedException {
        int size = 10;
        int totalPuts = 120;
        List<float[]> arrays = new ArrayList<>();

        for (int i = 0; i < totalPuts; i++) {
            // 如果队列满了，先消费一个再继续放
            if (buffer.getQueueSize() >= 50) {
                buffer.takeData();  // 丢弃一个旧数据，只保留循环复用逻辑
            }
            float[] arr = buffer.putFloatData(i, 0, size);
            arrays.add(arr);
        }

        // 验证位置指针循环（ARRAY_SIZE=60，所以第0和第60个应该是同一个数组对象）
        boolean hasReused = false;
        for (int i = 60; i < totalPuts; i++) {
            if (arrays.get(i) == arrays.get(i - 60)) {
                hasReused = true;
                break;
            }
        }
        assertTrue(hasReused, "Should reuse array slots after ARRAY_SIZE puts");
    }

    // ======================= Data 类型安全测试 =======================

    @Test
    void testGetFloatArrayFromShortDataThrows() {
        buffer.putShortData(100, 50, 256);
        Buffer.Data data = buffer.takeData();

        assertThrows(IllegalStateException.class, data::getFloatArray);
    }

    @Test
    void testGetShortArrayFromFloatDataThrows() {
        buffer.putFloatData(100, 50, 256);
        Buffer.Data data = buffer.takeData();

        assertThrows(IllegalStateException.class, data::getShortArray);
    }

    // ======================= 清空测试 =======================

    @Test
    void testClear() throws InterruptedException {
        buffer.putFloatData(1, 0, 10);
        buffer.putShortData(2, 0, 20);

        buffer.clear();

        // 队列应该为空
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Buffer.Data> future = executor.submit(() -> buffer.takeData());
        try {
            future.get(100, TimeUnit.MILLISECONDS);
            fail("Queue should be empty after clear");
        } catch (TimeoutException | ExecutionException e) {
            assertTrue(true);
        } finally {
            executor.shutdownNow();
        }

        // 位置指针应重置
        for (int i = 0; i < 5; i++) {
            float[] arr1 = buffer.putFloatData(0, 0, 10);
            short[] arr2 = buffer.putShortData(0, 0, 10);
            assertNotNull(arr1);
            assertNotNull(arr2);
        }
    }

    // ======================= 并发测试 =======================

    @Test
    void testConcurrentPutAndTake() throws InterruptedException {
        int producerThreads = 5;
        int consumerThreads = 5;
        int operationsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(producerThreads + consumerThreads);

        // 用于验证数据完整性
        ConcurrentLinkedQueue<Buffer.Data> takenData = new ConcurrentLinkedQueue<>();

        // 生产者
        Runnable producer = () -> {
            try {
                startLatch.await();
                for (int i = 0; i < operationsPerThread; i++) {
                    buffer.putFloatData(i, i / 2, 64);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finishLatch.countDown();
            }
        };

        // 消费者
        Runnable consumer = () -> {
            try {
                startLatch.await();
                for (int i = 0; i < operationsPerThread; i++) {
                    Buffer.Data data = buffer.takeData();
                    takenData.add(data);
                    assertNotNull(data.getFloatArray());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                finishLatch.countDown();
            }
        };

        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < producerThreads; i++) threads.add(new Thread(producer));
        for (int i = 0; i < consumerThreads; i++) threads.add(new Thread(consumer));
        threads.forEach(Thread::start);

        startLatch.countDown();
        boolean finished = finishLatch.await(10, TimeUnit.SECONDS);
        assertTrue(finished, "Not all threads finished within timeout");

        threads.forEach(t -> {
            try {
                t.join(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertEquals(producerThreads * operationsPerThread, takenData.size(),
                "Should consume all produced data");
    }

    @Test
    void testConcurrentPutDifferentTypes() throws InterruptedException {
        int operations = 200;
        CountDownLatch latch = new CountDownLatch(2);

        // Float producer
        Thread floatProducer = new Thread(() -> {
            for (int i = 0; i < operations; i++) {
                buffer.putFloatData(i, 0, 10);
            }
            latch.countDown();
        });

        // Short producer
        Thread shortProducer = new Thread(() -> {
            for (int i = 0; i < operations; i++) {
                buffer.putShortData(i, 0, 10);
            }
            latch.countDown();
        });

        floatProducer.start();
        shortProducer.start();

        // Consumer
        int totalExpected = operations * 2;
        for (int i = 0; i < totalExpected; i++) {
            Buffer.Data data = buffer.takeData();
            if (data.currentDataType() == Buffer.DataType.FLOAT) {
                assertNotNull(data.getFloatArray());
            } else {
                assertNotNull(data.getShortArray());
            }
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Producers should finish");
    }
}