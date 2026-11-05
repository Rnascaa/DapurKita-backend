package com.dapurkita.recipe;

import com.dapurkita.config.AppProperties;
import com.dapurkita.user.UserRepository;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Mengisi resep contoh agar demo dan uji UI tidak mulai dari daftar kosong.
 * Hanya aktif jika app.seed.sample-recipes=true (default: true di profil dev, tidak ada di production),
 * hanya berjalan saat tabel resep masih kosong, dan memakai akun admin sebagai pembuat.
 * gambar sengaja dikosongkan (imageUrl null) karena tidak ada sumber gambar yang bisa dipastikan.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed.sample-recipes", havingValue = "true")
public class SampleRecipeSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleRecipeSeeder.class);

    // id kategori dari V2__seed_categories.sql
    private static final long UTAMA = 1, LAUK = 2, SAYUR = 3, SOTO = 4, SAMBAL = 5, KUE = 6, JAJANAN = 7, MINUMAN = 8;

    private final RecipeRepository recipes;
    private final RecipeService service;
    private final UserRepository users;
    private final AppProperties props;

    public SampleRecipeSeeder(RecipeRepository recipes, RecipeService service, UserRepository users,
                              AppProperties props) {
        this.recipes = recipes;
        this.service = service;
        this.users = users;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (recipes.count() > 0) {
            return;
        }
        String email = props.admin().email();
        if (email == null || email.isBlank()) {
            log.warn("Seed resep dilewati: admin belum dikonfigurasi");
            return;
        }
        var admin = users.findByEmail(email.strip().toLowerCase(Locale.ROOT));
        if (admin.isEmpty()) {
            log.warn("Seed resep dilewati: akun admin tidak ditemukan");
            return;
        }
        List<RecipeRequest> samples = samples();
        // dibuat dari yang paling akhir agar resep pertama di daftar menjadi yang terbaru
        for (int i = samples.size() - 1; i >= 0; i--) {
            service.create(samples.get(i), admin.get().getId());
        }
        log.info("{} resep contoh dibuat", samples.size());
    }

    public static List<RecipeRequest> samples() {
        return List.of(
                recipe("Nasi Goreng Kampung",
                        "Nasi goreng sederhana berbumbu bawang, cabai, dan terasi. Cocok untuk sarapan.",
                        20, 2, UTAMA,
                        List.of(i("Nasi putih sisa semalam", "3", "piring"), i("Bawang merah", "4", "siung"),
                                i("Bawang putih", "2", "siung"), i("Cabai merah", "3", "buah"),
                                i("Terasi bakar", "1", "sdt"), i("Kecap manis", "2", "sdm"),
                                i("Telur ayam", "2", "butir"), i("Garam", "1/2", "sdt"),
                                i("Minyak goreng", "3", "sdm")),
                        List.of("Haluskan bawang merah, bawang putih, cabai merah, dan terasi.",
                                "Panaskan minyak, tumis bumbu halus sampai harum.",
                                "Geser bumbu ke tepi wajan, masukkan telur lalu orak-arik sampai matang.",
                                "Masukkan nasi, aduk rata dengan api besar.",
                                "Tambahkan kecap manis dan garam, aduk sampai nasi agak kering. Koreksi rasa dan sajikan hangat.")),
                recipe("Soto Ayam",
                        "Soto berkuah kuning bening dengan suwiran ayam, soun, dan tauge.",
                        60, 4, SOTO,
                        List.of(i("Daging ayam", "500", "g"), i("Air", "1,5", "liter"),
                                i("Serai", "2", "batang"), i("Daun salam", "2", "lembar"),
                                i("Daun jeruk", "3", "lembar"), i("Bawang merah", "6", "siung"),
                                i("Bawang putih", "4", "siung"), i("Kunyit", "2", "cm"), i("Jahe", "2", "cm"),
                                i("Kemiri", "3", "butir"), i("Merica butir", "1", "sdt"), i("Garam", "1,5", "sdt"),
                                i("Soun", "50", "g"), i("Tauge", "100", "g"), i("Telur rebus", "2", "butir"),
                                i("Seledri", "2", "batang"), i("Jeruk nipis", "1", "buah"),
                                i("Bawang goreng", "2", "sdm"), i("Minyak goreng", "2", "sdm")),
                        List.of("Rebus ayam bersama air, daun salam, daun jeruk, dan serai yang dimemarkan sampai empuk, sekitar 30 menit.",
                                "Angkat ayam, suwir dagingnya, dan saring kaldunya.",
                                "Haluskan bawang merah, bawang putih, kunyit, jahe, kemiri, dan merica.",
                                "Tumis bumbu halus dengan minyak sampai harum, lalu masukkan ke kaldu.",
                                "Didihkan kuah 15 menit, beri garam. Seduh soun dan tauge dengan air panas.",
                                "Susun soun, tauge, suwiran ayam, dan telur dalam mangkuk, siram kuah panas. Taburi seledri dan bawang goreng, beri perasan jeruk nipis.")),
                recipe("Rendang Daging Sapi",
                        "Daging sapi dimasak lama dalam santan dan rempah sampai kering dan berwarna gelap.",
                        240, 6, LAUK,
                        List.of(i("Daging sapi", "1", "kg"), i("Santan kental", "1", "liter"),
                                i("Cabai merah keriting", "150", "g"), i("Bawang merah", "10", "siung"),
                                i("Bawang putih", "6", "siung"), i("Jahe", "3", "cm"), i("Lengkuas", "4", "cm"),
                                i("Kunyit", "3", "cm"), i("Serai", "3", "batang"), i("Daun jeruk", "5", "lembar"),
                                i("Daun kunyit", "2", "lembar"), i("Asam kandis", "3", "keping"),
                                i("Kelapa parut sangrai", "3", "sdm"), i("Garam", "2", "sdt")),
                        List.of("Potong daging sapi menjadi kotak sekitar 4 cm.",
                                "Haluskan cabai, bawang merah, bawang putih, jahe, lengkuas, dan kunyit.",
                                "Masukkan bumbu halus, santan, serai, daun jeruk, daun kunyit, asam kandis, dan garam ke dalam wajan besar. Aduk sampai mendidih.",
                                "Masukkan daging, masak dengan api kecil sambil sesekali diaduk sekitar 2 jam sampai kuah menyusut.",
                                "Tambahkan kelapa sangrai. Lanjutkan memasak sambil terus diaduk sampai bumbu mengering dan minyak keluar, sekitar 1 jam lagi.",
                                "Koreksi rasa, angkat, dan sajikan dengan nasi hangat.")),
                recipe("Ayam Goreng Kuning",
                        "Ayam diungkep bumbu kunyit dan rempah, lalu digoreng sampai berkulit renyah.",
                        50, 4, LAUK,
                        List.of(i("Ayam potong", "1", "ekor"), i("Bawang putih", "5", "siung"),
                                i("Bawang merah", "4", "siung"), i("Kunyit", "3", "cm"), i("Ketumbar", "1", "sdt"),
                                i("Kemiri", "3", "butir"), i("Jahe", "2", "cm"), i("Lengkuas", "2", "cm"),
                                i("Serai", "1", "batang"), i("Daun salam", "2", "lembar"), i("Air", "300", "ml"),
                                i("Garam", "1,5", "sdt"), i("Minyak goreng", "500", "ml")),
                        List.of("Haluskan bawang putih, bawang merah, kunyit, ketumbar, kemiri, dan jahe.",
                                "Campurkan ayam dengan bumbu halus, lengkuas yang dimemarkan, serai, daun salam, garam, dan air.",
                                "Ungkep dengan api sedang sekitar 25 menit sampai ayam empuk dan air menyusut.",
                                "Tiriskan ayam. Panaskan minyak, goreng ayam sampai kuning kecokelatan.",
                                "Sajikan hangat dengan sambal dan lalapan.")),
                recipe("Gado-Gado",
                        "Sayuran rebus, telur, dan tahu dengan bumbu kacang gurih-manis.",
                        40, 4, SAYUR,
                        List.of(i("Kentang", "2", "buah"), i("Kol", "150", "g"), i("Kacang panjang", "100", "g"),
                                i("Tauge", "100", "g"), i("Bayam", "100", "g"), i("Telur rebus", "2", "butir"),
                                i("Tahu goreng", "2", "potong"), i("Timun", "1", "buah"),
                                i("Kacang tanah goreng", "150", "g"), i("Cabai rawit", "5", "buah"),
                                i("Bawang putih", "2", "siung"), i("Gula merah", "2", "sdm"),
                                i("Air asam jawa", "2", "sdm"), i("Garam", "1", "sdt"),
                                i("Air matang", "200", "ml"), i("Kerupuk", "8", "lembar")),
                        List.of("Rebus kentang sampai empuk, lalu kupas dan potong. Rebus kol, kacang panjang, tauge, dan bayam secara terpisah sebentar saja agar tetap segar.",
                                "Goreng sebentar kacang tanah jika belum matang, lalu haluskan bersama cabai rawit, bawang putih, gula merah, dan garam.",
                                "Campur bumbu dengan air matang dan air asam jawa sampai kental dan rata.",
                                "Tata sayuran, kentang, tahu, telur, dan timun di piring. Siram dengan bumbu kacang dan sajikan dengan kerupuk.")),
                recipe("Sayur Asem",
                        "Sayur berkuah asam-segar dengan jagung, labu siam, dan kacang panjang.",
                        45, 4, SAYUR,
                        List.of(i("Jagung manis", "1", "buah"), i("Kacang panjang", "100", "g"),
                                i("Labu siam", "1", "buah"), i("Melinjo", "50", "g"),
                                i("Asam jawa", "2", "sdm"), i("Air", "1,5", "liter"),
                                i("Bawang merah", "5", "siung"), i("Bawang putih", "2", "siung"),
                                i("Cabai merah", "2", "buah"), i("Kemiri", "1", "butir"),
                                i("Terasi", "1", "sdt"), i("Gula merah", "1", "sdm"), i("Garam", "1,5", "sdt"),
                                i("Daun salam", "2", "lembar"), i("Lengkuas", "2", "cm")),
                        List.of("Haluskan bawang merah, bawang putih, cabai, kemiri, dan terasi.",
                                "Didihkan air bersama bumbu halus, daun salam, lengkuas yang dimemarkan, dan asam jawa.",
                                "Masukkan jagung yang dipotong dan melinjo, masak sekitar 10 menit.",
                                "Masukkan labu siam dan kacang panjang, masak sampai empuk.",
                                "Beri gula merah dan garam, koreksi rasa. Sajikan hangat.")),
                recipe("Tumis Kangkung",
                        "Kangkung ditumis cepat dengan bawang, cabai, dan terasi.",
                        15, 2, SAYUR,
                        List.of(i("Kangkung", "2", "ikat"), i("Bawang putih", "3", "siung"),
                                i("Bawang merah", "2", "siung"), i("Cabai merah", "2", "buah"),
                                i("Terasi", "1/2", "sdt"), i("Saus tiram", "1", "sdm"), i("Garam", "1/4", "sdt"),
                                i("Minyak goreng", "2", "sdm"), i("Air", "2", "sdm")),
                        List.of("Siangi kangkung, potong sepanjang 5 cm, dan cuci bersih.",
                                "Iris bawang putih, bawang merah, dan cabai.",
                                "Tumis irisan bumbu dan terasi dengan minyak panas sampai harum.",
                                "Masukkan kangkung, air, saus tiram, dan garam. Aduk dengan api besar sekitar 2 menit sampai layu. Angkat segera agar tetap hijau.")),
                recipe("Sayur Bening Bayam",
                        "Sayur bening ringan dengan bayam dan jagung manis.",
                        20, 4, SAYUR,
                        List.of(i("Bayam", "2", "ikat"), i("Jagung manis", "1", "buah"),
                                i("Bawang merah", "4", "siung"), i("Bawang putih", "2", "siung"),
                                i("Air", "800", "ml"), i("Garam", "1", "sdt"), i("Gula pasir", "1/2", "sdt")),
                        List.of("Petik daun bayam dan cuci bersih. Pipil atau potong jagung.",
                                "Iris bawang merah dan bawang putih, rebus bersama air sampai mendidih.",
                                "Masukkan jagung, masak sekitar 8 menit sampai empuk.",
                                "Beri garam dan gula, masukkan bayam, masak 1 menit saja. Angkat dan sajikan hangat.")),
                recipe("Sambal Terasi",
                        "Sambal mentah-matang yang pedas dengan aroma terasi bakar.",
                        15, 4, SAMBAL,
                        List.of(i("Cabai rawit merah", "10", "buah"), i("Cabai merah keriting", "5", "buah"),
                                i("Bawang merah", "3", "siung"), i("Bawang putih", "2", "siung"),
                                i("Tomat", "1", "buah"), i("Terasi bakar", "1", "sdt"),
                                i("Gula pasir", "1", "sdt"), i("Garam", "1/2", "sdt"),
                                i("Jeruk limau", "1", "buah"), i("Minyak goreng", "3", "sdm")),
                        List.of("Goreng cabai, bawang merah, bawang putih, dan tomat sebentar sampai layu.",
                                "Ulek kasar semua bahan bersama terasi, gula, dan garam.",
                                "Peras jeruk limau, aduk rata, dan koreksi rasa. Sajikan segera.")),
                recipe("Bakso Sapi Kuah",
                        "Bakso kenyal dari daging sapi giling dalam kuah kaldu hangat dengan mi dan sawi.",
                        90, 6, SOTO,
                        List.of(i("Daging sapi giling", "500", "g"), i("Tepung tapioka", "100", "g"),
                                i("Es batu", "100", "g"), i("Bawang putih", "4", "siung"),
                                i("Merica bubuk", "1", "sdt"), i("Garam", "2", "sdt"),
                                i("Baking powder", "1/2", "sdt"), i("Kaldu sapi", "1,5", "liter"),
                                i("Mi kuning", "200", "g"), i("Sawi hijau", "100", "g"),
                                i("Seledri", "2", "batang"), i("Bawang goreng", "2", "sdm")),
                        List.of("Haluskan bawang putih. Proses daging giling dengan es batu, bawang putih, merica, dan garam sampai lengket dan halus.",
                                "Campur tepung tapioka dan baking powder, uleni sebentar sampai adonan kalis.",
                                "Didihkan air lalu kecilkan api sampai hangat. Bentuk adonan bulat dengan tangan, masukkan ke air hangat.",
                                "Naikkan api, rebus sampai bakso mengapung, lalu angkat.",
                                "Panaskan kaldu sapi, masukkan bakso, beri garam secukupnya.",
                                "Seduh mi kuning dan sawi, susun dalam mangkuk, siram dengan kuah dan bakso. Taburi seledri dan bawang goreng.")),
                recipe("Klepon",
                        "Bola ketan hijau berisi gula merah cair, digulingkan dalam kelapa parut.",
                        60, 6, KUE,
                        List.of(i("Tepung ketan", "200", "g"), i("Daun pandan", "5", "lembar"),
                                i("Air", "150", "ml"), i("Garam", "1/4", "sdt"), i("Gula merah sisir", "100", "g"),
                                i("Kelapa parut", "150", "g"), i("Garam untuk kelapa", "1/2", "sdt")),
                        List.of("Blender daun pandan dengan air, saring sampai didapat air pandan.",
                                "Campur tepung ketan dan garam, tuang air pandan sedikit demi sedikit sambil diuli sampai bisa dibentuk.",
                                "Ambil sedikit adonan, pipihkan, isi gula merah, lalu bulatkan rapat.",
                                "Rebus bola ketan di air mendidih sampai mengapung, angkat dan tiriskan.",
                                "Kukus kelapa parut bersama garam sekitar 10 menit. Gulingkan klepon dalam kelapa sebelum disajikan.")),
                recipe("Dadar Gulung",
                        "Dadar hijau tipis berisi kelapa dan gula merah.",
                        45, 6, KUE,
                        List.of(i("Tepung terigu", "150", "g"), i("Telur ayam", "1", "butir"),
                                i("Santan", "250", "ml"), i("Air pandan", "100", "ml"), i("Garam", "1/4", "sdt"),
                                i("Minyak goreng", "1", "sdm"), i("Kelapa parut", "150", "g"),
                                i("Gula merah", "100", "g"), i("Gula pasir", "2", "sdm"),
                                i("Air", "100", "ml"), i("Daun pandan", "1", "lembar")),
                        List.of("Aduk tepung terigu, telur, santan, air pandan, dan garam sampai halus tanpa gumpalan.",
                                "Masak gula merah, gula pasir, air, dan daun pandan sampai larut, masukkan kelapa parut, aduk sampai kering. Dinginkan.",
                                "Panaskan wajan antilengket yang diolesi minyak tipis, tuang adonan tipis-tipis sampai matang di satu sisi.",
                                "Beri isian kelapa, gulung rapat. Ulangi sampai adonan habis.")),
                recipe("Pisang Goreng",
                        "Pisang kepok dibalut adonan tepung renyah, digoreng sampai keemasan.",
                        25, 4, JAJANAN,
                        List.of(i("Pisang kepok matang", "6", "buah"), i("Tepung terigu", "100", "g"),
                                i("Tepung beras", "2", "sdm"), i("Gula pasir", "2", "sdm"),
                                i("Garam", "1/4", "sdt"), i("Air es", "150", "ml"),
                                i("Minyak goreng", "500", "ml")),
                        List.of("Kupas pisang, belah menjadi dua memanjang.",
                                "Aduk tepung terigu, tepung beras, gula, garam, dan air es sampai kental dan halus.",
                                "Panaskan minyak. Celupkan pisang ke adonan, goreng dengan api sedang sampai keemasan.",
                                "Tiriskan dan sajikan selagi hangat.")),
                recipe("Tempe Mendoan",
                        "Tempe tipis berbalut tepung berbumbu dan daun bawang, digoreng setengah matang.",
                        25, 4, JAJANAN,
                        List.of(i("Tempe", "2", "papan"), i("Tepung terigu", "100", "g"),
                                i("Tepung beras", "2", "sdm"), i("Daun bawang", "3", "batang"),
                                i("Bawang putih", "2", "siung"), i("Ketumbar bubuk", "1/2", "sdt"),
                                i("Garam", "1", "sdt"), i("Air", "150", "ml"), i("Minyak goreng", "500", "ml")),
                        List.of("Iris tempe tipis lebar. Haluskan bawang putih, iris halus daun bawang.",
                                "Campur tepung terigu, tepung beras, bawang putih, ketumbar, garam, daun bawang, dan air sampai adonan agak encer.",
                                "Celupkan tempe ke adonan sampai terbalut rata.",
                                "Goreng dalam minyak panas sebentar saja, sekitar 1 menit tiap sisi, sampai setengah matang dan masih lemas. Sajikan dengan cabai rawit.")),
                recipe("Wedang Jahe",
                        "Minuman jahe hangat dengan serai dan gula merah.",
                        15, 2, MINUMAN,
                        List.of(i("Jahe", "5", "cm"), i("Serai", "1", "batang"), i("Gula merah", "3", "sdm"),
                                i("Air", "500", "ml"), i("Cengkih", "2", "butir")),
                        List.of("Bakar jahe sebentar lalu memarkan. Memarkan serai.",
                                "Rebus air bersama jahe, serai, dan cengkih sampai mendidih.",
                                "Masukkan gula merah, aduk sampai larut. Kecilkan api, masak 5 menit.",
                                "Saring dan sajikan hangat.")),
                recipe("Telur Balado",
                        "Telur rebus digoreng sebentar dan dibalur sambal balado pedas.",
                        25, 4, LAUK,
                        List.of(i("Telur rebus", "6", "butir"), i("Cabai merah keriting", "8", "buah"),
                                i("Cabai rawit", "5", "buah"), i("Bawang merah", "5", "siung"),
                                i("Bawang putih", "3", "siung"), i("Tomat", "1", "buah"),
                                i("Gula pasir", "1", "sdt"), i("Garam", "1", "sdt"),
                                i("Jeruk nipis", "1/2", "buah"), i("Minyak goreng", "4", "sdm")),
                        List.of("Goreng telur rebus sebentar sampai kulitnya sedikit mengerut. Tiriskan.",
                                "Haluskan cabai merah, cabai rawit, bawang merah, bawang putih, dan tomat secara kasar.",
                                "Tumis bumbu dengan minyak sampai matang dan harum, beri gula dan garam.",
                                "Masukkan telur, aduk sampai terbalut bumbu. Peras jeruk nipis dan angkat."))
        );
    }

    private static RecipeRequest recipe(String title, String description, int minutes, int servings, long categoryId,
                                        List<IngredientRequest> ingredients, List<String> steps) {
        return new RecipeRequest(title, description, null, minutes, servings, categoryId, ingredients,
                steps.stream().map(InstructionRequest::new).toList());
    }

    private static IngredientRequest i(String name, String quantity, String unit) {
        return new IngredientRequest(name, quantity, unit);
    }
}
