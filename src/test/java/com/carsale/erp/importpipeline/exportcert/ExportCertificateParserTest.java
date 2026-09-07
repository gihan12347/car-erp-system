package com.carsale.erp.importpipeline.exportcert;

import com.carsale.erp.account.User;
import com.carsale.erp.shared.vehicle.Vehicle;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.carsale.erp.importpipeline.auction.AuctionParseResult;

class ExportCertificateParserTest {

    private final ExportCertificateParser parser = new ExportCertificateParser();

    @Test
    void parseExtractsEnglishExportCertificate() {
        String text = "Certificate No: 00884\n"
                + "Arrangement No: 5051215834190374\n"
                + "Export Certificate\n"
                + "Registration No.: Toyama 583 ro 9037\n"
                + "Date of Registration: 27/02/2025\n"
                + "First Reg. Date: 01/2025\n"
                + "Chassis No.: JF5-1141982\n"
                + "Trademark of the maker: Honda\n"
                + "Model: 6BA-JF5 [296]\n"
                + "Engine Model: S07B\n"
                + "Name of User: M.D.K Corporation Ltd.\n"
                + "Address of User: 678-1 Tsubatae, Imizu City, Toyama Prefecture\n"
                + "Name of Owner: Same as user\n"
                + "Address of Owner: Same as user address\n"
                + "Locality of principal abode of use: Same as user address\n"
                + "Classification of Vehicle: Light Vehicle\n"
                + "Use: Passenger Use\n"
                + "Purpose: Private\n"
                + "Type of Body: Station Wagon [003]\n"
                + "Fixed Number: 4 Person\n"
                + "Maxim. Carry: -kg\n"
                + "Weight: 920kg\n"
                + "G/Weight: 1140kg\n"
                + "Engine Capacity: 0.65KW/L\n"
                + "Classification of Fuel: Petrol\n"
                + "Specification No.: 20749\n"
                + "Classification No.: 0002\n"
                + "Length: 339cm\n"
                + "Width: 147cm\n"
                + "Height: 179cm\n"
                + "FF Weight: 550kg\n"
                + "FR Weight: -kg\n"
                + "RF Weight: -kg\n"
                + "RR Weight: 370kg\n"
                + "Export scheduled day: 26/08/2025\n"
                + "Issue Date: 27/02/2025\n";

        AuctionParseResult result = parser.parse(text);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFields()).containsEntry("documentType", "English");
        assertThat(result.getFields()).containsEntry("certificateNo", "00884");
        assertThat(result.getFields()).containsEntry("arrangementNo", "5051215834190374");
        assertThat(result.getFields()).containsEntry("registrationNo", "Toyama 583 ro 9037");
        assertThat(result.getFields()).containsEntry("chassisVin", "JF5-1141982");
        assertThat(result.getFields()).containsEntry("make", "Honda");
        assertThat(result.getFields()).containsEntry("model", "6BA-JF5");
        assertThat(result.getFields()).containsEntry("engineModel", "S07B");
        assertThat(result.getFields()).containsEntry("vehicleClassification", "Light Vehicle");
        assertThat(result.getFields()).containsEntry("useType", "Passenger");
        assertThat(result.getFields()).containsEntry("purpose", "Private");
        assertThat(result.getFields()).containsEntry("bodyType", "Station Wagon");
        assertThat(result.getFields()).containsEntry("seatingCapacity", "4");
        assertThat(result.getFields()).containsEntry("weightKg", "920");
        assertThat(result.getFields()).containsEntry("grossWeightKg", "1140");
        assertThat(result.getFields()).containsEntry("lengthCm", "339");
        assertThat(result.getFields()).containsEntry("widthCm", "147");
        assertThat(result.getFields()).containsEntry("heightCm", "179");
        assertThat(result.getFields()).containsEntry("fuelType", "Petrol");
        assertThat(result.getFields()).containsEntry("frontAxleWeight", "550");
        assertThat(result.getFields()).containsEntry("rearAxleWeight", "370");
        assertThat(result.getFields()).containsEntry("exportScheduledDate", "26/08/2025");
        assertThat(result.getFields()).containsEntry("userName", "M.D.K Corporation Ltd.");
    }

    @Test
    void parseExtractsJapaneseExportCertificateAndNormalizesValues() {
        String text = "番号 00884\n"
                + "整理番号 5051215834190374\n"
                + "輸出予定届出証明書 Export Certificate\n"
                + "車両番号 富山 583 ろ 9037\n"
                + "交付年月日 令和 7 年 2月 27日\n"
                + "初度検査年月 令和 7 年 1月\n"
                + "自動車の種別 軽自動車\n"
                + "用途 乗用\n"
                + "自家用・事業用の別 自家用\n"
                + "車体の形状 ステーションワゴン [003]\n"
                + "車台番号 JF5-1141982\n"
                + "乗車定員 4人\n"
                + "最大積載量 - kg\n"
                + "車両重量 920 kg\n"
                + "車両総重量 1140 kg\n"
                + "長さ 339 cm\n"
                + "幅 147 cm\n"
                + "高さ 179 cm\n"
                + "車名 ホンダ\n"
                + "型式 6BA-JF5\n"
                + "原動機の型式 S07B\n"
                + "燃料の種別 ガソリン\n"
                + "総排気量又は定格出力 0.65 L\n"
                + "前前軸重 550 kg\n"
                + "後後軸重 370 kg\n"
                + "型式指定番号 20749\n"
                + "類別区分番号 0002\n"
                + "使用者の氏名又は名称 有限会社 M. D. K Corporation\n"
                + "使用者の住所 富山県射水市津幡江６７８－１\n"
                + "所有者の氏名又は名称 使用者に同じ\n"
                + "所有者の住所 使用者住所に同じ\n"
                + "使用の本拠の位置 使用者住所に同じ\n"
                + "輸出予定日 令和 7 年 8月 26日\n"
                + "備考 燃料効率基準達成\n";

        AuctionParseResult result = parser.parse(text);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFields()).containsEntry("documentType", "Japanese");
        assertThat(result.getFields()).containsEntry("certificateNo", "00884");
        assertThat(result.getFields()).containsEntry("arrangementNo", "5051215834190374");
        assertThat(result.getFields()).containsEntry("chassisVin", "JF5-1141982");
        assertThat(result.getFields()).containsEntry("make", "Honda");
        assertThat(result.getFields()).containsEntry("model", "6BA-JF5");
        assertThat(result.getFields()).containsEntry("engineModel", "S07B");
        assertThat(result.getFields()).containsEntry("vehicleClassification", "Light Vehicle");
        assertThat(result.getFields()).containsEntry("useType", "Passenger");
        assertThat(result.getFields()).containsEntry("purpose", "Private");
        assertThat(result.getFields()).containsEntry("bodyType", "Station Wagon");
        assertThat(result.getFields()).containsEntry("weightKg", "920");
        assertThat(result.getFields()).containsEntry("grossWeightKg", "1140");
        assertThat(result.getFields()).containsEntry("fuelType", "Petrol");
        assertThat(result.getFields()).containsEntry("frontAxleWeight", "550");
        assertThat(result.getFields()).containsEntry("rearAxleWeight", "370");
        assertThat(result.getFields()).containsEntry("registrationDate", "27/02/2025");
        assertThat(result.getFields()).containsEntry("firstRegDate", "01/2025");
        assertThat(result.getFields()).containsEntry("exportScheduledDate", "26/08/2025");
        assertThat(result.getFields()).containsEntry("specificationNo", "20749");
    }
}
