package org.balinhui.fpaplayer.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LyricsTest {

    @Test
    public void test() {
        TreeMap<Long, List<String>> treeMap = Lyrics.parse("[ti:Onegai☆Snyaiper]\r\n" +
                "[ar:沢城みゆき (さわしろ みゆき)]\r\n" +
                "[al:Yes,my master my lord / Onegai☆Snyaiper]\r\n" +
                "[00:00.00]おねがい☆すにゃいぱー - 泽城美雪 (沢城みゆき)\r\n" + //0
                "[00:07.70]词：辻純更\r\n" + //7070
                "[00:15.41]曲：鈴木一史\r\n" + //15041
                "[00:23.12]ベイビーベイベー"); //23012

        assertEquals("おねがい☆すにゃいぱー - 泽城美雪 (沢城みゆき)", treeMap.get(0L).getFirst());
        assertEquals("词：辻純更", treeMap.get(7070L).getFirst());
        assertEquals("曲：鈴木一史", treeMap.get(15041L).getFirst());
        assertEquals("ベイビーベイベー", treeMap.get(23012L).getFirst());
    }

}