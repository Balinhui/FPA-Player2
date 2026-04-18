package org.balinhui.fpaplayer.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class FlacCoverExtractor {
    private static final Logger log = LogManager.getLogger(FlacCoverExtractor.class);

    private FlacCoverExtractor() {}

    /**
     * 获取缺失mimetype的文件的封面，如果有
     * @param flacPath 文件路径
     * @return 图像的数组，没有则为null
     * @throws IOException 找不到文件或无法正常处理
     */
    public static byte[] extractFlacCover(String flacPath) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(flacPath)))) {
            // --- 1. 检查 FLAC 文件头 ---
            byte[] header = new byte[4];
            dis.readFully(header);
            if (!new String(header, StandardCharsets.US_ASCII).equals("fLaC")) {
                log.warn("不是有效的 FLAC 文件");
                return null;
            }

            boolean isLast = false;
            while (!isLast) {
                int blockHeader = dis.readUnsignedByte(); // 1 byte
                isLast = (blockHeader & 0x80) != 0; // high bit is last-metadata-block flag
                int blockType = blockHeader & 0x7F; // lower 7 bits
                int blockLength = (dis.readUnsignedByte() << 16) | (dis.readUnsignedByte() << 8) | dis.readUnsignedByte();

                if (blockType == 6) {
                    // --- 2. Found METADATA_BLOCK_PICTURE ---
                    byte[] pictureData = new byte[blockLength];
                    dis.readFully(pictureData);
                    byte[] data = parseAndSavePicture(pictureData);
                    log.info("封面提取成功");
                    return data;
                } else {
                    dis.skipBytes(blockLength); // skip non-picture blocks
                }
            }

            log.warn("未找到封面图片");
            return null;
        }
    }

    private static byte[] parseAndSavePicture(byte[] data) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data));

        dis.readInt(); // picture type (ignored)
        int mimeLen = dis.readInt();
        byte[] mimeBytes = new byte[mimeLen];
        dis.readFully(mimeBytes);

        int descLen = dis.readInt();
        dis.skipBytes(descLen); // skip description

        dis.skipBytes(4 * 4); // width, height, color depth, indexed colors

        int picLen = dis.readInt();
        byte[] imageData = new byte[picLen];
        dis.readFully(imageData);

        return imageData;
    }
}
