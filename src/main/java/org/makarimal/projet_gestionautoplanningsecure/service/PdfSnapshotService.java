package org.makarimal.projet_gestionautoplanningsecure.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;

@Service
public class PdfSnapshotService {
    private static final int TIMEOUT_MS = 180_000;

    public byte[] capture(String url, String jwt, long companyId) {
        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium()
                    .launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext ctx = browser.newContext();
            Page page = ctx.newPage();

            // 1) Appliquer un timeout global
            page.setDefaultNavigationTimeout(TIMEOUT_MS);
            page.setDefaultTimeout(TIMEOUT_MS);

            // 2) Injection du token + user
            page.addInitScript(String.format("""
        window.localStorage.setItem('auth_token', '%s');
        window.localStorage.setItem('current_user',
          JSON.stringify({
            id: 0,
            firstName: 'System',
            lastName:  'Account',
            roles: ['ADMIN'],
            companyId: %d
          })
        );
      """, jwt, companyId));

            // 3) Navigation + attente NETWORKIDLE
            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // 4) Attendre que la table soit visible
            page.waitForSelector("table#planning",
                    new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

            // 5) Debug : capture d’écran pour inspecter le DOM final
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get("debug.png"))
                    .setFullPage(true));
            System.out.println(">> debug screenshot saved to debug.png");

            // 6) Tenter d’attendre les détails site, mais sans planter si c’est introuvable
            Locator details = page.locator("div.site-details .site-line");
            if (details.count() > 0) {
                details.first().waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE));
            } else {
                System.out.println("⚠️  Aucune ligne .site-line trouvée, on passe la génération du PDF quand même.");
            }

            // 7) Génération du PDF
            return page.pdf(new Page.PdfOptions()
                    .setFormat("A3")
                    .setLandscape(true)
                    .setScale(0.9)
                    .setPrintBackground(true));

        } catch (PlaywrightException e) {
            throw new RuntimeException("Échec de génération du PDF", e);
        }
    }
}
