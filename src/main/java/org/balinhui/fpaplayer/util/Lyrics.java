package org.balinhui.fpaplayer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lyrics {

    private static final Pattern lyricsPattern = Pattern.compile("lyrics", java.util.regex.Pattern.CASE_INSENSITIVE);

    private static final Pattern timePattern = Pattern.compile("\\[(\\d{2}:\\d{2}\\.\\d{2,3})]");

    private Lyrics() {}

    public static TreeMap<Long, List<String>> parse(Map<String, String> metadata) {
        String lyrics = "[00:02.00]纯音乐，请欣赏";
        for (String s : metadata.keySet()) {
            Matcher matcher = lyricsPattern.matcher(s);
            if (matcher.find()) {
                lyrics = metadata.get(s);
                break;
            }
        }
        return parse(lyrics);
    }

    public static TreeMap<Long, List<String>> parse(String lyrics) {
        TreeMap<Long, List<String>> output = new TreeMap<>();

        String[] lines = lyrics.split("\r?\n");
        for (String line : lines) {
            Matcher matcher = timePattern.matcher(line);
            if (matcher.find()) {
                long mTime = timeToMillis(matcher.group(1));
                String lyric = matcher.replaceFirst("").trim();
                if (allSpace(lyric)) continue;
                if (output.containsKey(mTime)) {
                    output.get(mTime).add(lyric);
                } else {
                    output.put(mTime, new ArrayList<>(List.of(lyric)));
                }
            }
        }
        return output;
    }

    private static long timeToMillis(String timeStr) {
        String[] parts = timeStr.split("[:.]");
        long minutes = Long.parseLong(parts[0]);
        int seconds = Integer.parseInt(parts[1]);
        int millis = Integer.parseInt(parts[2]);

        return (minutes * 60 + seconds) * 1000 + millis;
    }

    private static boolean allSpace(String lyric) {
        for (int i = 0; i < lyric.length(); i++) {
            if (lyric.charAt(i) != ' ') return false;
        }
        return true;
    }
}
