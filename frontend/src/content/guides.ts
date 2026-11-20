export interface Guide {
  category: string;
  title: string;
  summary: string;
  searchQuery: string;
  imageUrl: string;
  imageAlt: string;
}

export const guides: Guide[] = [
  {
    category: "Robot süpürgeler",
    title: "Temizliği biraz hafiflet.",
    summary: "Günlük işlere küçük bir yardımcı.",
    searchQuery: "robot süpürge",
    imageUrl: "/discover/robot-vacuum.jpg",
    imageAlt: "Dreame D20 beyaz robot süpürge",
  },
  {
    category: "Airfryer modelleri",
    title: "Mutfakta yeni bir alışkanlık.",
    summary: "Tezgâhına ve ihtiyacına uygun modeli bul.",
    searchQuery: "airfryer",
    imageUrl: "/discover/airfryer.jpg",
    imageAlt: "Philips 3000 Serisi NA322/00 airfryer",
  },
  {
    category: "Kulaklıklar",
    title: "Biraz da kendini dinle.",
    summary: "Yolda, işte, günün sana kalan kısmında.",
    searchQuery: "kulaklık",
    imageUrl: "/discover/headphones.jpg",
    imageAlt: "Sony WH-1000XM5 siyah kulak üstü kulaklık",
  },
];
