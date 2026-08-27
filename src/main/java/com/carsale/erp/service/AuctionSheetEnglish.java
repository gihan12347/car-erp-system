package com.carsale.erp.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class AuctionSheetEnglish {

    private static final Map<String, String> PHRASES = new LinkedHashMap<String, String>();
    private static final List<Entry<String, String>> ORDERED = new ArrayList<Entry<String, String>>();
    private static final String[] MONTHS = new String[] {
            "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };

    static {
        put("全方位モニター用カメラパッケージ", "All-around monitor camera package");
        put("スズキコネクト対応通信機装着車", "Suzuki Connect unit fitted");
        put("スズキセーフティーサポート", "Suzuki Safety Support");
        put("デュアルセンサーブレーキサポート", "Dual sensor brake support");
        put("両側パワースライドドア", "Dual power sliding doors");
        put("両側パワースライド", "Dual power sliding doors");
        put("パワースライドドア", "Power sliding door");
        put("パワーウィンドウ", "Power windows");
        put("パワーウインドウ", "Power windows");
        put("バックモニター", "Backup monitor");
        put("全方位モニター", "All-around monitor");
        put("スマートアシスト", "Smart Assist");
        put("プッシュスタート", "Push start");
        put("スペアスマートキー", "Spare smart key");
        put("スマートキー×2", "2 smart keys");
        put("スマートキーx2", "2 smart keys");
        put("スマートキー×２", "2 smart keys");
        put("新車保証書", "New-car warranty");
        put("取扱説明書", "Owner's manual");
        put("車両取説", "Vehicle manual");
        put("ワンオーナー", "One owner");
        put("オートオークション", "Auto Auction");
        put("ヘッドライトアセ", "Headlight fading");
        put("ハンドルハゲ", "Steering wheel peeling");
        put("室内薄汚れ", "Interior slightly dirty");
        put("外装小傷有り", "Small exterior scratches");
        put("小傷有り", "Small scratches");
        put("ホイールカバー", "Wheel cover");
        put("エンドパネル", "End panel");
        put("ステレオレス", "No stereo");
        put("後日発送部品", "Parts to be sent later");
        put("後日発送", "To be sent later");
        put("初出品", "First listing");
        put("純正装備", "Factory equipment");
        put("ランドクルーザー", "Land Cruiser");
        put("アルファード", "Alphard");
        put("ヴォクシー", "Voxy");
        put("エスティマ", "Estima");
        put("ハリアー", "Harrier");
        put("カローラアクシオ", "Corolla Axio");
        put("ツーリングセレクション", "Touring Selection");
        put("ツーリング セレクション", "Touring Selection");
        put("アクシオ", "Corolla Axio");
        put("カローラ", "Corolla");
        put("シエンタ", "Sienta");
        put("ノア", "Noah");
        put("クラウン", "Crown");
        put("プリウス", "Prius");
        put("ルーミー", "Roomy");
        put("ハイブリッドZ", "Hybrid Z");
        put("ハイブリッドＺ", "Hybrid Z");
        put("ハイブリッド", "Hybrid");
        put("パールホワイト", "Pearl White");
        put("ガソリン", "Gasoline");
        put("ディーゼル", "Diesel");
        put("エアバッグ", "Airbag");
        put("エアバック", "Airbag");
        put("パワステ", "Power steering");
        put("キーレス", "Keyless");
        put("保証書", "Warranty");
        put("取説", "Manual");
        put("ナビ", "Navigation");
        put("アルミ", "Alloy wheels");
        put("自家用", "Private");
        put("レンタ", "Rental");
        put("事業用", "Commercial");
        put("ワゴンＲ", "Wagon R");
        put("ワゴンR", "Wagon R");
        put("スペーシア", "Spacia");
        put("ハスラー", "Hustler");
        put("ジムニー", "Jimny");
        put("ヴェゼル", "Vezel");
        put("ベゼル", "Vezel");
        put("フィット", "Fit");
        put("セレナ", "Serena");
        put("ノート", "Note");
        put("デイズ", "Days");
        put("デミオ", "Demio");
        put("タント", "Tanto");
        put("ムーヴ", "Move");
        put("ロッキー", "Rocky");
        put("アクア", "Aqua");
        put("ヤリス", "Yaris");
        put("ディスプレイオーディオ", "Display audio");
        put("パノラミックビューモニター", "Panoramic view monitor");
        put("フロントシートヒーター", "Front seat heaters");
        put("シートヒーター", "Seat heaters");
        put("LEDヘッドライト", "LED headlights");
        put("オートクルーズ", "Auto cruise");
        put("ハイプリッド", "Hybrid");
        put("ライズ", "Raize");
        put("ライス", "Raize");
        put("ヴィッツ", "Vitz");
        put("パッソ", "Passo");
        put("タンク", "Tank");
        put("アルト", "Alto");
        put("トヨタ", "Toyota");
        put("ホンダ", "Honda");
        put("ニッサン", "Nissan");
        put("日産", "Nissan");
        put("マツダ", "Mazda");
        put("スバル", "Subaru");
        put("スズキ", "Suzuki");
        put("ダイハツ", "Daihatsu");
        put("三菱", "Mitsubishi");
        put("レクサス", "Lexus");
        put("ホワイト", "White");
        put("ブラック", "Black");
        put("シルバー", "Silver");
        put("グレー", "Gray");
        put("レッド", "Red");
        put("ブルー", "Blue");
        put("ベージュ", "Beige");
        put("ブラウン", "Brown");
        put("グリーン", "Green");
        put("パール", "Pearl");
        put("ワイン", "Wine");
        put("軽油", "Diesel");
        put("電気", "Electric");
        put("無段", "CVT");
        put("自動", "AT");
        put("マニュアル", "MT");
        put("オート", "AT");
        put("エアコン", "A/C");
        put("冷房", "A/C");
        put("5ドア", "5-door");
        put("４ドア", "4-door");
        put("4ドア", "4-door");
        put("4SD", "4-door");
        put("5SD", "5-door");
        put("5W", "5-door");
        put("3ドア", "3-door");
        put("５ハコ", "5-door box");
        put("5ハコ", "5-door box");
        put("ハコ", "box");
        put("京都", "Kyoto");
        put("車内", "in vehicle");
        put("フロア", "floor");
        put("Hライトアセ", "Headlight fading");
        put("エアB", "Airbag");
        put("ＩアB", "Airbag");
        put("白", "White");
        put("黒", "Black");
        put("銀", "Silver");
        put("灰", "Gray");
        put("赤", "Red");
        put("青", "Blue");
        put("茶", "Brown");
        put("緑", "Green");
        put("円", " yen");
        put("人", "");

        ORDERED.addAll(PHRASES.entrySet());
        Collections.sort(ORDERED, new Comparator<Entry<String, String>>() {
            public int compare(Entry<String, String> left, Entry<String, String> right) {
                return right.getKey().length() - left.getKey().length();
            }
        });
    }

    private static void put(String japanese, String english) {
        PHRASES.put(japanese, english);
    }

    static String convert(String value) {
        if (value == null) {
            return null;
        }
        String result = convertDates(value);
        result = result.replace('（', '(').replace('）', ')').replace('、', ',');
        Iterator<Entry<String, String>> phrases = ORDERED.iterator();
        while (phrases.hasNext()) {
            Entry<String, String> entry = phrases.next();
            result = result.replace(entry.getKey(), entry.getValue());
        }
        result = result.replaceAll("[\\u3040-\\u30FF\\u4E00-\\u9FFF]", " ");
        result = result.replaceAll("[|｜]+", " ");
        result = result.replaceAll("(?<=[A-Za-z0-9])\\(", " (");
        result = result.replaceAll("\\s+,", ",");
        result = result.replaceAll("(?i)HybridZ", "Hybrid Z");
        result = result.replaceAll("\\s+", " ").trim();
        result = result.replaceAll("(^,\\s*)+|(\\s*,)+$", "");
        return result.length() == 0 ? null : result;
    }

    private static String convertDates(String value) {
        Matcher era = Pattern.compile("(令和|平成|昭和)\\s*(\\d{1,2}|元)\\s*年\\s*(\\d{1,2})\\s*月").matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (era.find()) {
            int year = toWesternYear(era.group(1), era.group(2));
            int month = Integer.parseInt(era.group(3));
            String english = monthName(month) + " " + year;
            era.appendReplacement(buffer, Matcher.quoteReplacement(english));
        }
        era.appendTail(buffer);
        return buffer.toString();
    }

    private static int toWesternYear(String era, String number) {
        int n = "元".equals(number) ? 1 : Integer.parseInt(number);
        if ("令和".equals(era)) {
            return 2018 + n;
        }
        if ("平成".equals(era)) {
            return 1988 + n;
        }
        return 1925 + n;
    }

    private static String monthName(int month) {
        if (month >= 1 && month <= 12) {
            return MONTHS[month];
        }
        return String.valueOf(month);
    }

    private AuctionSheetEnglish() {
    }
}
