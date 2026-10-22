import { BotIcon, CookingPotIcon, HeadphonesIcon, type LucideIcon } from "lucide-react";

export interface Guide {
  title: string;
  summary: string;
  searchQuery: string;
  icon: LucideIcon;
}

export const guides: Guide[] = [
  {
    title: "Robot süpürge seçerken nelere bakmalı?",
    summary: "Emiş gücü, haritalama ve fiyat aralığına göre modelleri karşılaştır.",
    searchQuery: "robot süpürge",
    icon: BotIcon,
  },
  {
    title: "Airfryer alırken bakman gereken 5 şey",
    summary: "Hazne hacmi, güç ve temizlik kolaylığı.",
    searchQuery: "airfryer",
    icon: CookingPotIcon,
  },
  {
    title: "Gürültü engelleyici kulaklık rehberi",
    summary: "Pil ömrü, konfor ve gürültü engelleme.",
    searchQuery: "kulaklık",
    icon: HeadphonesIcon,
  },
];
