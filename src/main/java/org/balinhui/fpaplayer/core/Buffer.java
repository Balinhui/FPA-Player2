package org.balinhui.fpaplayer.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Buffer {

    private static final int BUFFER_SIZE = 50;
    private static final int ARRAY_SIZE = BUFFER_SIZE + 10;

    private static final BlockingQueue<Data> queue = new LinkedBlockingQueue<>(BUFFER_SIZE);
    private static final List<short[]> shortData = Collections.synchronizedList(new ArrayList<>());
    private static final List<float[]> floatData = Collections.synchronizedList(new ArrayList<>());

    private static final Buffer instance = new Buffer();

    private final AtomicInteger putFloatPos = new AtomicInteger(0);
    private final AtomicInteger putShortPos = new AtomicInteger(0);

    private Buffer() {
        if (instance != null)
            throw new RuntimeException("非正常获取实例");
    }

    public static Buffer getInstance() {
        return instance;
    }

    public float[] putFloatData(int samplesNumber, int oldSamplesNumber, int size) {
        if (floatData.isEmpty()) {
            synchronized (floatData) {
                if (floatData.isEmpty())
                    initFloatArray(size);
            }
        }


        int pos = putFloatPos.getAndUpdate(operand -> (operand + 1) % ARRAY_SIZE);

        synchronized (floatData) {
            float[] array = floatData.get(pos);
            if (array.length <= size) {
                floatData.set(pos, new float[size]);
            }
        }

        Data data = new Data(samplesNumber, oldSamplesNumber, pos, DataType.FLOAT, false);
        try {
            queue.put(data);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("队列操作被中断", e);
        }
        return floatData.get(pos);
    }

    private void initFloatArray(int size) {
        for (int i = 0; i < ARRAY_SIZE; i++) {
            floatData.add(new float[size]);
        }
    }

    public short[] putShortData(int samplesNumber, int oldSamplesNumber, int size) {
        if (shortData.isEmpty()) {
            synchronized (shortData) {
                if (shortData.isEmpty()) {
                    initShortData(size);
                }
            }
        }

        int pos = putShortPos.getAndUpdate(p -> (p + 1) % ARRAY_SIZE);

        synchronized (shortData) {
            short[] array = shortData.get(pos);
            if (array.length <= size) {
                shortData.set(pos, new short[size]);
            }
        }

        Data data = new Data(samplesNumber, oldSamplesNumber, pos, DataType.SHORT, false);
        try {
            queue.put(data);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return shortData.get(pos);
    }

    private void initShortData(int size) {
        for (int i = 0; i < ARRAY_SIZE; i++) {
            shortData.add(new short[size]);
        }
    }

    public void putEndInfo(int count) {
        Data data = new Data(0, 0, count, null, true);
        try {
            queue.put(data);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Data takeData() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("从缓冲区获取数据被中断", e);
        }
    }

    public int getQueueSize() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public void clear() {
        queue.clear();
        putFloatPos.set(0);
        putShortPos.set(0);
    }

    public record Data(int samplesNumber, int oldSamplesNumber, int pos, DataType currentDataType, boolean end) {
        public float[] getFloatArray() {
            if (end) return null;
            if (currentDataType != DataType.FLOAT) {
                throw new IllegalStateException(
                        String.format("需求类型与当前类型不符: 需要 FLOAT, 实际 %s", currentDataType)
                );
            }
            return floatData.get(pos);
        }

        public short[] getShortArray() {
            if (end) return null;
            if (currentDataType != DataType.SHORT) {
                throw new IllegalStateException(
                        String.format("需求类型与当前类型不符: 需要 SHORT, 实际 %s", currentDataType)
                );
            }
            return shortData.get(pos);
        }
    }

    public enum DataType {
        SHORT, FLOAT
    }
}
