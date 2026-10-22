import { SiteFooter } from "@/components/layout/site-footer";
import { SiteHeader } from "@/components/layout/site-header";

export default function SiteLayout({ children }: { children: React.ReactNode }) {
  return (
    <>
      <SiteHeader />
      <main
        id="main-content"
        tabIndex={-1}
        className="mx-auto w-full max-w-[1240px] flex-1 px-5 py-10 outline-none sm:px-8 lg:py-12"
      >
        {children}
      </main>
      <SiteFooter />
    </>
  );
}
