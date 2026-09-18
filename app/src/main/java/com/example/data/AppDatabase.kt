package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.ChatMessageDao
import com.example.data.dao.MarketingPostDao
import com.example.data.dao.VeoVideoDao
import com.example.data.dao.VideoScriptDao
import com.example.data.dao.WhatsAppCampaignDao
import com.example.data.model.AudienceType
import com.example.data.model.ChatMessage
import com.example.data.model.ContentCategory
import com.example.data.model.ContentLanguage
import com.example.data.model.ContentTone
import com.example.data.model.GeneratedVeoVideo
import com.example.data.model.MarketingPost
import com.example.data.model.Platform
import com.example.data.model.PostStatus
import com.example.data.model.UserRole
import com.example.data.model.VideoScript
import com.example.data.model.WhatsAppCampaign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MarketingPost::class,
        VideoScript::class,
        WhatsAppCampaign::class,
        ChatMessage::class,
        GeneratedVeoVideo::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): MarketingPostDao
    abstract fun videoScriptDao(): VideoScriptDao
    abstract fun whatsappCampaignDao(): WhatsAppCampaignDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun veoVideoDao(): VeoVideoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rvh_marketing_studio.db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                getInstance(context).seedInitialData()
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

    suspend fun seedInitialData() {
        val now = System.currentTimeMillis()
        val oneDayMillis = 86400000L

        // Initial Demo Marketing Posts
        val samplePosts = listOf(
            MarketingPost(
                title = "Sunday Pet Vaccination & Deworming Camp",
                platform = Platform.FACEBOOK.name,
                contentType = "Post",
                category = ContentCategory.VACCINATION.name,
                audience = AudienceType.PET_OWNERS.name,
                serviceOrProduct = "DHPPiL & Anti-Rabies Vaccine",
                tone = ContentTone.PROMOTIONAL.name,
                language = ContentLanguage.HINGLISH.name,
                ctaText = "Book Consultation",
                status = PostStatus.APPROVED.name,
                contentText = "🐾 Rohit Veterinary House Special Sunday Pet Camp! 🐶🐱\n\nIs your dog or cat due for their annual booster? Don't let Parvovirus or Rabies put your beloved companion at risk!\n\n📅 This Sunday | 9:00 AM - 2:00 PM\n✅ Free Basic Clinical Checkup with every vaccination\n✅ High quality cold-chain assured vaccines\n✅ Experienced veterinarian consultation\n\n📍 Location: Rohit Veterinary House, Main Road\n📞 Call / WhatsApp: +91 98765 43210\n\nPrior appointment recommended to avoid wait times!",
                shortVersion = "🐾 Special Pet Vaccination Camp this Sunday at Rohit Veterinary House! Free basic checkup with annual shots. Book appointment now: +91 98765 43210.",
                longVersion = "🐾 Protect Your Furry Family Member: Rohit Veterinary House Sunday Vaccination Drive!\n\nVaccination is not just a routine procedure; it is a life-saving shield against deadly illnesses like Canine Parvovirus, Distemper, Hepatitis, Leptospirosis, and Rabies.\n\nAt Rohit Veterinary House, we maintain strict WHO-standard cold-chain storage for all biologicals, ensuring 100% potency.\n\n✨ Camp Highlights:\n- Complete Physical Examination (Weight, Heart rate, Fur/Skin)\n- Deworming guidance based on exact weight\n- Anti-Rabies & 7-in-1 / 9-in-1 combo vaccines\n- Nutritional diet counselling for Indian climatic conditions\n\nVisit us this Sunday. Protect early, protect well!",
                alternate1 = "Dog & Cat parents, mark your calendars! Sunday Vaccination Drive at Rohit Veterinary House. Keep your pets protected from Parvo & Rabies. Call +91 98765 43210.",
                alternate2 = "Apne pet ka vaccination time par karwayen! Rohit Veterinary House me is Ravivaar special camp. Free health examination with vaccine. Book now!",
                alternate3 = "Don't delay your pet's shots. Parvovirus cases spike this season. Safe, gentle, professional vaccination at Rohit Veterinary House. Call +91 98765 43210.",
                hashtags = "#RohitVeterinaryHouse #PetCare #DogVaccination #CatHealth #VeterinaryClinic #PetParents #ParvoPrevention",
                imagePrompt = "A smiling friendly veterinarian in a clean modern clinic gently examining a happy Golden Retriever puppy, soft warm lighting, professional veterinary banner style, high resolution.",
                videoPrompt = "Close up of veterinarian checking a playful puppy's ears and heartbeat, text overlay: 'Annual Pet Booster Reminder', transition to clinic exterior logo.",
                scheduledDateMillis = now + (oneDayMillis * 2),
                createdDateMillis = now - (oneDayMillis * 3),
                leadsGenerated = 14,
                clicks = 68,
                authorRole = UserRole.MARKETING_STAFF.name
            ),
            MarketingPost(
                title = "दुधारू पशुओं में हीट स्ट्रेस और दूध में गिरावट से बचाव",
                platform = Platform.WHATSAPP.name,
                contentType = "Promotional Message",
                category = ContentCategory.CATTLE_HEALTH.name,
                audience = AudienceType.CATTLE_OWNERS.name,
                serviceOrProduct = "Chelated Mineral Mixture & Electrolytes",
                tone = ContentTone.EDUCATIONAL.name,
                language = ContentLanguage.HINDI.name,
                ctaText = "WhatsApp Now",
                status = PostStatus.SCHEDULED.name,
                contentText = "🐄 *पशुपालक भाइयों के लिए जरूरी सलाह - रोहित वेटरनरी हाउस* 🥛\n\nगर्मी और उमस के मौसम में गाय-भैंसों में हांफना, चारा कम खाना और दूध उत्पादन 15-30% तक घट जाना आम समस्या है।\n\n*बचाव के 4 अचूक उपाय:*\n1. पशुओं को ठंडे व हवादार स्थान पर रखें और दिन में 3-4 बार ताजा पानी पिलाएं।\n2. चारे में नियमित *चिलेटेड मिनरल मिक्सचर (Chelated Mineral Mixture)* 50 ग्राम प्रतिदिन दें।\n3. गर्मी के तनाव से बचाने के लिए इलेक्ट्रोलाइट्स व विटामिन-सी युक्त सप्लीमेंट दें।\n4. हरा चारा और सूखा चारा 3:1 के अनुपात में दें।\n\nरोहित वेटरनरी हाउस पर प्रमाणित मिनरल मिक्सचर, मिल्क बूस्टर और वेटनरी सप्लीमेंट्स उचित मूल्य पर उपलब्ध हैं।\n\n📲 परामर्श व आर्डर हेतु संपर्क करें: *+91 98765 43210*\n📍 रोहित वेटरनरी हाउस, मुख्य बाजार",
                shortVersion = "🐄 गर्मी में गाय-भैंस का दूध घटने न दें! संतुलित मिनरल मिक्सचर और ठंडे पानी का प्रबंध करें। रोहित वेटरनरी हाउस: +91 98765 43210.",
                longVersion = "🐄 दुधारू पशुओं में हीट स्ट्रेस नियंत्रण गाइड - रोहित वेटरनरी हाउस 🌾\n\nउच्च तापमान और नमी के कारण पशुओं की लार टपकना, सांस तेज चलना और दूध की फैट व एसएनएफ कम हो जाती है।\n\nरोहित वेटरनरी हाउस की विशेष सलाह:\n- सुबह और शाम के समय ही दाना मिश्रण दें।\n- नमक और सोडा (मीठा सोडा) चारे में संतुलित मात्रा में मिलाएं।\n- पेट के कीड़ों (डीवर्मिंग) की दवा हर 3 महीने में अवश्य दें।\n\nकिसी भी बीमारी या परामर्श के लिए रोहित वेटरनरी हाउस की हेल्पलाइन पर संपर्क करें।",
                alternate1 = "गाय-भैंस चारा कम खा रही है? हीट स्ट्रेस का लक्षण हो सकता है। तुरंत रोहित वेटरनरी हाउस से इलेक्ट्रोलाइट्स व मिनरल्स लें: +91 98765 43210.",
                alternate2 = "डेयरी किसान भाई ध्यान दें: गर्मी में दूध उत्पादन स्थिर रखने के लिए गुणवत्तापूर्ण मिनरल सप्लीमेंट अपनाएं। रोहित वेटरनरी हाउस।",
                alternate3 = "गर्मियों में पशु स्वास्थ्य सुरक्षा - रोहित वेटरनरी हाउस द्वारा प्रमाणित पोषक तत्व उपलब्ध हैं। कॉल करें +91 98765 43210.",
                hashtags = "#DairyFarming #CattleCare #RohitVeterinaryHouse #Pashupalan #MilkBooster #VetMedicine #BiharDairy",
                imagePrompt = "A healthy Jersey cow eating fresh green fodder under a ventilated dairy shed, vibrant morning daylight, professional agricultural banner.",
                videoPrompt = "Slow pan of dairy cow resting in airy shed, farmer filling mineral mix into feed bucket, text in Hindi: 'हीट स्ट्रेस से बचाव के उपाय'.",
                scheduledDateMillis = now + (oneDayMillis * 1),
                createdDateMillis = now - (oneDayMillis * 1),
                leadsGenerated = 22,
                clicks = 94,
                authorRole = UserRole.ADMIN.name
            ),
            MarketingPost(
                title = "Monsoon Mastitis Prevention Guidelines",
                platform = Platform.FACEBOOK.name,
                contentType = "Post",
                category = ContentCategory.SEASONAL.name,
                audience = AudienceType.CATTLE_OWNERS.name,
                serviceOrProduct = "Teat Dip Sanitizer & Intramammary Infusions",
                tone = ContentTone.EDUCATIONAL.name,
                language = ContentLanguage.ENGLISH.name,
                ctaText = "Call Now",
                status = PostStatus.PUBLISHED.name,
                contentText = "⚠️ Dairy Farmers Alert: Monsoon Season Increases Mastitis Risk by 40%!\n\nWet and muddy floors are breeding grounds for environmental bacteria causing Subclinical and Clinical Mastitis in high-yielding dairy cows and buffaloes.\n\n🛡️ Prevention Protocol from Rohit Veterinary House:\n1. Keep shedding floors dry with agricultural lime powder.\n2. Practice regular Pre and Post milking Teat Dip sanitation.\n3. Do not allow animals to sit down immediately after milking for 30 minutes.\n4. Screen milk regularly with California Mastitis Test (CMT).\n\nIf you observe swelling, clots in milk, or hot udders, contact our clinic immediately for prompt antibiotic sensitivity testing and supportive therapy.\n\n📞 Emergency / Consult: +91 98765 43210\nRohit Veterinary House - Trusted Animal Healthcare Partner",
                shortVersion = "Monsoon dampness triggers Mastitis! Keep shed dry, use teat dip post milking. For emergency care, contact Rohit Veterinary House: +91 98765 43210.",
                longVersion = "Protect your dairy investment: Comprehensive Mastitis management by Rohit Veterinary House...",
                alternate1 = "Prevent costly Mastitis this rainy season. Clean teat dips & bedding hygiene make all the difference. Rohit Veterinary House: +91 98765 43210.",
                alternate2 = "Udders swollen or watery milk? Don't wait until damage becomes permanent. Contact Rohit Veterinary House veterinary team now!",
                alternate3 = "Dairy protection tips: Teat hygiene is key to zero mastitis loss. Consult Rohit Veterinary House experts today.",
                hashtags = "#MastitisPrevention #DairyHealth #RohitVeterinary #CattleCare #VeterinaryMedicine #FarmerEducation",
                imagePrompt = "Clean dairy farm milking station with modern teat dipping cup demonstration, crisp focus, educational vet graphic.",
                videoPrompt = "Close-up of clean milking hygiene steps with informative graphic pointers.",
                scheduledDateMillis = now - (oneDayMillis * 4),
                createdDateMillis = now - (oneDayMillis * 5),
                publishedDateMillis = now - (oneDayMillis * 4),
                leadsGenerated = 31,
                clicks = 112,
                authorRole = UserRole.MARKETING_STAFF.name
            ),
            MarketingPost(
                title = "Goat Farming Monsoon Deworming Campaign",
                platform = Platform.WHATSAPP.name,
                contentType = "Status Text",
                category = ContentCategory.DEWORMING.name,
                audience = AudienceType.GOAT_FARMERS.name,
                serviceOrProduct = "Broad-Spectrum Albendazole / Ivermectin Suspension",
                tone = ContentTone.URGENT.name,
                language = ContentLanguage.HINGLISH.name,
                ctaText = "Order Product",
                status = PostStatus.DRAFT.name,
                contentText = "🐐 *बकरीपालक भाइयों के लिए जरूरी सूचना - रोहित वेटरनरी हाउस* 🌿\n\nबरसात के मौसम में बकरियों में पेट के कीड़े (Bottle Jaw / Liver Fluke) तेजी से फैलते हैं, जिससे वजन घटना और अचानक मृत्यु दर बढ़ जाती है।\n\n⚠️ लक्षण:\n- जबड़े के नीचे पानी जैसी सूजन (Bottle Jaw)\n- दस्त या पतला गोबर\n- बाल रूखे व बेजान होना\n\n✅ समय पर सही खुराक में डीवर्मिंग कराएं। गर्भवती बकरियों के लिए सुरक्षित दवाएं उपलब्ध हैं।\n\nदवा मंगाने या जानकारी हेतु व्हाट्सएप करें: *+91 98765 43210*",
                shortVersion = "🐐 बकरियों में बरसात के कीड़ों से बचाव: समय पर डीवर्मिंग कराएं। सही दवा हेतु संपर्क करें रोहित वेटरनरी हाउस: +91 98765 43210.",
                longVersion = "Goat farmers alert: Complete Deworming & Vaccination protocol for rainy season...",
                alternate1 = "बकरी के जबड़े के नीचे सूजन है? यह लिवर फ्लूक का लक्षण हो सकता है। तुरंत रोहित वेटरनरी हाउस से संपर्क करें।",
                alternate2 = "बकरी पालन में मुनाफा तभी जब बकरियां स्वस्थ रहें! मानसून डीवर्मिंग डोज आज ही लें। कॉल: +91 98765 43210.",
                alternate3 = "Monsoon goat care deworming medicine available at Rohit Veterinary House. Protect your herd.",
                hashtags = "#GoatFarming #BakriPalan #RohitVeterinaryHouse #VeterinarySupport #DewormingDose",
                imagePrompt = "Healthy Black Bengal goats grazing on pasture under veterinary supervision, clean rural setting.",
                videoPrompt = "Veterinarian showing proper oral drenching technique for goats with dosing syringe.",
                scheduledDateMillis = null,
                createdDateMillis = now - (oneDayMillis * 1),
                leadsGenerated = 0,
                clicks = 0,
                authorRole = UserRole.CONTENT_CREATOR.name
            ),
            MarketingPost(
                title = "Instant 100% Guaranteed Cure for All Animal Diseases",
                platform = Platform.FACEBOOK.name,
                contentType = "Post",
                category = ContentCategory.PRODUCT_PROMO.name,
                audience = AudienceType.GENERAL.name,
                serviceOrProduct = "Miracle Tonic",
                tone = ContentTone.PROMOTIONAL.name,
                language = ContentLanguage.ENGLISH.name,
                ctaText = "Call Now",
                status = PostStatus.REJECTED.name,
                contentText = "Buy this tonic and cure all cattle and dog problems in 10 minutes guaranteed!",
                shortVersion = "Instant cure tonic for animals.",
                longVersion = "Instant cure tonic for animals...",
                alternate1 = "",
                alternate2 = "",
                alternate3 = "",
                hashtags = "#MiracleCure",
                imagePrompt = "",
                videoPrompt = "",
                scheduledDateMillis = null,
                createdDateMillis = now - (oneDayMillis * 2),
                leadsGenerated = 0,
                clicks = 0,
                authorRole = UserRole.CONTENT_CREATOR.name,
                rejectionReason = "Rejected by Admin: Violates professional veterinary ethics. Medical claims must be scientifically sound, evidence-based, and free of exaggerated guarantees."
            ),
            MarketingPost(
                title = "Poultry Farm Biosecurity & Monsoon Electrolyte Protocol",
                platform = Platform.WHATSAPP.name,
                contentType = "Broadcast Message",
                category = ContentCategory.POULTRY_CARE.name,
                audience = AudienceType.POULTRY_FARMERS.name,
                serviceOrProduct = "Water Sanitizer & Stress-Relief Electrolytes",
                tone = ContentTone.EDUCATIONAL.name,
                language = ContentLanguage.HINGLISH.name,
                ctaText = "Call Now",
                status = PostStatus.PENDING_APPROVAL.name,
                contentText = "🐔 *Poultry Farmers Alert: Monsoon Coccidiosis & Moisture Management*\n\nExcess humidity during rainy season can increase litter dampness and mortality. Ensure clean drinking water sanitation daily with veterinary-grade sanitizers.\n\n🛡️ Key Checklist:\n1. Keep litter dry by turning and adding fresh lime.\n2. Add stress-relief electrolytes in drinking water.\n3. Vaccinate flock against Ranikhet (RD) and IBD on schedule.\n\nFor quality poultry biologicals and vitamins, contact Rohit Veterinary House: +91 98765 43210.",
                shortVersion = "🐔 Protect your poultry flock from dampness and disease. Clean water sanitation & electrolytes available at Rohit Veterinary House.",
                longVersion = "Comprehensive poultry biosecurity guide for broiler and layer farmers...",
                alternate1 = "Poultry care: Keep drinking water bacteria-free this monsoon. Rohit Veterinary House helpline: +91 98765 43210.",
                alternate2 = "Barish me murgiyo me bimari se bachav ke liye water sanitizer aur electrolytes ka prayog karein. Rohit Veterinary House.",
                alternate3 = "Reduce poultry mortality with veterinary certified biosecurity solutions. Contact Rohit Veterinary House today.",
                hashtags = "#PoultryFarming #RohitVeterinaryHouse #BroilerHealth #LayerFarming #BioSecurity",
                imagePrompt = "Clean well-ventilated poultry farm shed with healthy chickens, high-definition agricultural photography.",
                videoPrompt = "Poultry expert explaining water sanitation drops in clean overhead tank.",
                scheduledDateMillis = null,
                createdDateMillis = now - (oneDayMillis * 1),
                leadsGenerated = 0,
                clicks = 0,
                authorRole = UserRole.CONTENT_CREATOR.name
            ),
            MarketingPost(
                title = "Calcium Gel for Post-Calving Dairy Cattle (Reels)",
                platform = Platform.VIDEO.name,
                contentType = "Video Script",
                category = ContentCategory.CATTLE_HEALTH.name,
                audience = AudienceType.CATTLE_OWNERS.name,
                serviceOrProduct = "Ionic Calcium Gel Supplement",
                tone = ContentTone.URGENT.name,
                language = ContentLanguage.HINDI.name,
                ctaText = "WhatsApp Now",
                status = PostStatus.SCHEDULED.name,
                contentText = "🐄 *ब्यांत के बाद गाय-भैंस में मिल्क फीवर (Milk Fever) से बचाव*\n\nबच्चा देने के 24-48 घंटे के भीतर दुधारू पशुओं में कैल्शियम की भारी कमी हो जाती है जिससे पशु बैठ जाता है और खड़ा नहीं हो पाता।\n\n✅ ब्यांत के तुरंत बाद रोहित वेटरनरी हाउस का प्रमाणित ओरल कैल्शियम जेल अवश्य पिलाएं।\n\nआपातकालीन सहायता व डिलीवरी हेतु व्हाट्सएप करें: +91 98765 43210",
                shortVersion = "🐄 मिल्क फीवर से बचाएं अपनी गाय-भैंस को! ब्यांत के तुरंत बाद ओरल कैल्शियम जेल दें। रोहित वेटरनरी हाउस: +91 98765 43210.",
                longVersion = "Post-calving calcium management guidelines by Rohit Veterinary House veterinary specialists...",
                alternate1 = "गाय ब्याने के बाद कमजोर पड़ रही है? तुरंत ओरल कैल्शियम जेल दें। संपर्क: +91 98765 43210.",
                alternate2 = "Milk fever can cause permanent dairy loss. Keep calcium gel ready before calving date. Rohit Veterinary House.",
                alternate3 = "ब्यांत के बाद दुग्ध बुखार से सुरक्षा: रोहित वेटरनरी हाउस पर उच्च गुणवत्ता वाला कैल्शियम जेल उपलब्ध।",
                hashtags = "#MilkFever #DairyCattle #CalciumGel #RohitVeterinaryHouse #PashuSwasthya",
                imagePrompt = "Veterinarian holding oral calcium bottle next to healthy dairy cow and calf, farm setting, warm morning light.",
                videoPrompt = "Reel showing symptoms of hypocalcemia in cattle and how oral gel is safely administered.",
                scheduledDateMillis = now + (oneDayMillis * 3),
                createdDateMillis = now - (oneDayMillis * 2),
                leadsGenerated = 18,
                clicks = 54,
                authorRole = UserRole.MARKETING_STAFF.name
            )
        )
        postDao().insertPosts(samplePosts)

        // Initial Demo Video Scripts
        val sampleScripts = listOf(
            VideoScript(
                title = "30-Sec Reel: Stop Cow Milk Drop in Peak Summer!",
                category = ContentCategory.CATTLE_HEALTH.name,
                durationSec = 30,
                hookText = "Kya garmi me aapki gay-bhains ka doodh 2-3 litre kam ho gaya hai? (Dramatic pause with sad farmer visual)",
                sceneBreakdownJson = """[
                    {"scene": 1, "duration": "0-5s", "visual": "Hot sun, cow panting in stall, farmer holding empty milk bucket", "onscreen": "दूध कम हो गया?", "audio": "गर्मी में गाय का दूध अचानक कम क्यों होता है? जानिए मुख्य कारण!"},
                    {"scene": 2, "duration": "5-15s", "visual": "Veterinarian at Rohit Veterinary House pointing to mineral mix and water tub", "onscreen": "हीट स्ट्रेस के 3 उपाय", "audio": "पहला: 24 घंटे ठंडा पानी, दूसरा: चारे में 50g चिलेटेड मिनरल मिक्सचर, और तीसरा: मीठा सोडा!"},
                    {"scene": 3, "duration": "15-25s", "visual": "Cow happily eating green fodder, full foaming milk container", "onscreen": "दूध और फैट दोनों बढ़ेगा!", "audio": "रोहित वेटरनरी हाउस के वैज्ञानिक सप्लीमेंट्स से पशु रहेगा तंदुरुस्त और दूध रहेगा भरपूर!"},
                    {"scene": 4, "duration": "25-30s", "visual": "Rohit Veterinary House clinic banner with phone number", "onscreen": "कॉल करें: +91 98765 43210", "audio": "आज ही संपर्क करें रोहित वेटरनरी हाउस या व्हाट्सएप पर आर्डर करें!"}
                ]""",
                voiceoverText = "गर्मी में गाय का दूध अचानक कम क्यों होता है? जानिए मुख्य कारण! पहला: 24 घंटे ठंडा पानी, दूसरा: चारे में 50 ग्राम चिलेटेड मिनरल मिक्सचर, और तीसरा: मीठा सोडा! रोहित वेटरनरी हाउस के वैज्ञानिक सप्लीमेंट्स से पशु रहेगा तंदुरुस्त और दूध रहेगा भरपूर! आज ही संपर्क करें रोहित वेटरनरी हाउस या व्हाट्सएप पर आर्डर करें!",
                onScreenText = "हीट स्ट्रेस से दूध कम? 3 आसान उपाय! 1. ठंडा पानी 2. मिनरल मिक्स 3. इलेक्ट्रोलाइट्स | रोहित वेटरनरी हाउस +91 98765 43210",
                ctaEnding = "Call / WhatsApp Rohit Veterinary House: +91 98765 43210",
                aspectRatio = "9:16",
                status = PostStatus.APPROVED.name,
                createdDateMillis = now - (oneDayMillis * 2)
            ),
            VideoScript(
                title = "15-Sec Status: 3 Signs Your Puppy Needs Deworming",
                category = ContentCategory.PET_CARE.name,
                durationSec = 15,
                hookText = "Puppy's tummy looking swollen like a pot? Watch this! 🐶",
                sceneBreakdownJson = """[
                    {"scene": 1, "duration": "0-4s", "visual": "Close-up cute puppy with bloated belly", "onscreen": "Pot Belly = Worms?", "audio": "Is your puppy eating well but still looks bloated with dull fur?"},
                    {"scene": 2, "duration": "4-10s", "visual": "Infographic showing roundworms and safe deworming drops", "onscreen": "Safe Deworming", "audio": "These are clear symptoms of intestinal worms! Deworming is essential every month till 6 months."},
                    {"scene": 3, "duration": "10-15s", "visual": "Puppy jumping happily, Rohit Veterinary House logo", "onscreen": "Consult RVH Today!", "audio": "Get veterinarian-approved puppy dewormers at Rohit Veterinary House. Visit us today!"}
                ]""",
                voiceoverText = "Is your puppy eating well but still looks bloated with dull fur? These are clear symptoms of intestinal worms! Deworming is essential every month till 6 months. Get veterinarian-approved puppy dewormers at Rohit Veterinary House. Visit us today!",
                onScreenText = "3 Puppy Worm Signs: 1. Pot Belly 2. Dull Fur 3. Scooting | Get Safe Dewormer at Rohit Veterinary House",
                ctaEnding = "Visit Rohit Veterinary House or call +91 98765 43210",
                aspectRatio = "9:16",
                status = PostStatus.DRAFT.name,
                createdDateMillis = now - (oneDayMillis * 1)
            ),
            VideoScript(
                title = "60-Sec Educational: Goat Enterotoxemia (Fidki) Prevention",
                category = ContentCategory.GOAT_FARMING.name,
                durationSec = 60,
                hookText = "बकरियों में फिड़की (ET) से अचानक होने वाली मौतों को कैसे रोकें?",
                sceneBreakdownJson = """[
                    {"scene": 1, "duration": "0-10s", "visual": "Goat farm in morning, veterinarian discussing disease with farmer", "onscreen": "फिड़की रोग से सावधान", "audio": "बरसात के शुरू होते ही नया हरा चारा खाने से बकरियों में फिड़की रोग यानी एंटरोटॉक्सेमिया का खतरा कई गुना बढ़ जाता है।"},
                    {"scene": 2, "duration": "10-25s", "visual": "Animation of bacterial toxin and symptoms: rolling eyes, bloating", "onscreen": "बीमारी के लक्षण", "audio": "इसमें स्वस्थ बकरी भी 2-4 घंटे के अंदर तड़पने लगती है और इलाज का समय भी नहीं मिल पाता।"},
                    {"scene": 3, "duration": "25-45s", "visual": "Veterinarian administering ET vaccine to goat herd", "onscreen": "एकमात्र उपाय: ET टीका", "audio": "बचाव का सबसे असरदार और सस्ता तरीका है समय पर ET का टीका लगवाना। मानसून से ठीक पहले सभी बकरियों का टीकाकरण जरूर कराएं।"},
                    {"scene": 4, "duration": "45-60s", "visual": "Rohit Veterinary House clinic and emergency medicine shelf", "onscreen": "रोहित वेटरनरी हाउस", "audio": "रोहित वेटरनरी हाउस पर कोल्ड चेन मेंटेन की हुई ET वैक्सीन और प्राथमिक उपचार दवाएं हर समय उपलब्ध हैं। अपनी बकरियों की सुरक्षा के लिए आज ही संपर्क करें।"}
                ]""",
                voiceoverText = "बरसात के शुरू होते ही नया हरा चारा खाने से बकरियों में फिड़की रोग यानी एंटरोटॉक्सेमिया का खतरा कई गुना बढ़ जाता है। इसमें स्वस्थ बकरी भी 2-4 घंटे के अंदर तड़पने लगती है और इलाज का समय भी नहीं मिल पाता। बचाव का सबसे असरदार और सस्ता तरीका है समय पर ET का टीका लगवाना। रोहित वेटरनरी हाउस पर कोल्ड चेन मेंटेन की हुई ET वैक्सीन और प्राथमिक उपचार दवाएं हर समय उपलब्ध हैं। अपनी बकरियों की सुरक्षा के लिए आज ही संपर्क करें।",
                onScreenText = "बकरियों में फिड़की (ET) से बचाव: समय पर टीका लगवाएं | रोहित वेटरनरी हाउस +91 98765 43210",
                ctaEnding = "रोहित वेटरनरी हाउस से संपर्क करें: +91 98765 43210",
                aspectRatio = "9:16",
                status = PostStatus.APPROVED.name,
                createdDateMillis = now - (oneDayMillis * 3)
            )
        )
        videoScriptDao().insertVideoScripts(sampleScripts)

        // Initial WhatsApp Campaigns
        val sampleCampaigns = listOf(
            WhatsAppCampaign(
                campaignName = "Dairy Farmers Summer Booster Drive",
                targetGroup = AudienceType.CATTLE_OWNERS.name,
                templateBody = "नमस्ते {{CustomerName}} जी! रोहित वेटरनरी हाउस की तरफ से आपके {{AnimalType}} के लिए विशेष सूचना: गर्मी में दूध उत्पादन बनाए रखने के लिए प्रमाणित चिलेटेड मिनरल मिक्सचर उपलब्ध है। परामर्श हेतु संपर्क करें।",
                buttonCtaType = "WhatsApp Now",
                buttonCtaValue = "+91 98765 43210",
                totalRecipients = 240,
                sentCount = 240,
                failedCount = 4,
                responseCount = 46,
                status = PostStatus.PUBLISHED.name,
                scheduledDateMillis = now - (oneDayMillis * 2),
                createdDateMillis = now - (oneDayMillis * 3)
            ),
            WhatsAppCampaign(
                campaignName = "Weekend Pet Health & Deworming Notice",
                targetGroup = AudienceType.PET_OWNERS.name,
                templateBody = "Dear {{CustomerName}}, seasonal ticks and worms are active in our area. Bring your {{AnimalType}} to Rohit Veterinary House for preventive spot-on treatment and clinical checkup.",
                buttonCtaType = "Book Consultation",
                buttonCtaValue = "+91 98765 43210",
                totalRecipients = 110,
                sentCount = 110,
                failedCount = 2,
                responseCount = 31,
                status = PostStatus.APPROVED.name,
                scheduledDateMillis = now + (oneDayMillis * 2),
                createdDateMillis = now - (oneDayMillis * 1)
            ),
            WhatsAppCampaign(
                campaignName = "Poultry Monsoon Biosecurity & Vitamin Alert",
                targetGroup = AudienceType.POULTRY_FARMERS.name,
                templateBody = "पोल्ट्री फार्मर भाइयों के लिए जरूरी संदेश: मानसून में कॉक्सीडियोसिस और नमी से बचाव के लिए शेड सैनिटाइजर व वाटर सैनिटाइजर का प्रयोग करें। उत्पाद रोहित वेटरनरी हाउस पर उपलब्ध हैं।",
                buttonCtaType = "Call Now",
                buttonCtaValue = "+91 98765 43210",
                totalRecipients = 75,
                sentCount = 0,
                failedCount = 0,
                responseCount = 0,
                status = PostStatus.DRAFT.name,
                scheduledDateMillis = null,
                createdDateMillis = now
            )
        )
        whatsappCampaignDao().insertCampaigns(sampleCampaigns)

        // Initial Chat Assistant Message
        val initialChat = ChatMessage(
            sender = "ASSISTANT",
            text = "Namaste! I am your AI Marketing Assistant at Rohit Veterinary House. 🐾🐄\n\nI can help you craft high-converting, medically safe marketing copy for WhatsApp, Facebook, and Reels in Hindi, Hinglish, or English.\n\nTo get started, tell me: Which platform is this content for (WhatsApp, Facebook, or Video)?",
            timestamp = now
        )
        chatMessageDao().insertMessage(initialChat)
    }
}
