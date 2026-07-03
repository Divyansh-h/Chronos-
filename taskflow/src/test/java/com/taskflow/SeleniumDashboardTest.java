package com.taskflow;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SeleniumDashboardTest {

    private WebDriver driver;

    @BeforeEach
    public void setUp() {
        // Automatically downloads and configures the correct ChromeDriver binary
        WebDriverManager.chromedriver().setup();
        
        ChromeOptions options = new ChromeOptions();
        // Run in headless mode to ensure it doesn't crash in CI environments without a display
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");
        
        driver = new ChromeDriver(options);
    }

    @Test
    public void verifyDashboardStatCardsRenderCorrectly() {
        try {
            // Act: Navigate to the running Vite frontend
            driver.get("http://localhost:5173");

            // Arrange: Setup an explicit wait since React needs a few milliseconds to paint the DOM
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h1[text()='Dashboard']")));

            // Assert 1: Locate the StatCards using the global 'glass-panel' utility class
            List<WebElement> statCards = driver.findElements(By.cssSelector(".glass-panel"));
            assertTrue(statCards.size() >= 4, "Expected at least 4 glass-panel StatCards to render on the Dashboard");

            // Assert 2: Verify the 'Total Workflows' card text exists and is physically visible
            WebElement totalWorkflowsHeader = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//h3[contains(text(), 'Total Workflows')]")));
            assertTrue(totalWorkflowsHeader.isDisplayed(), "Total Workflows header is missing");

            // Assert 3: Verify the 'Live Server Status' card text is visible
            WebElement serverStatusHeader = driver.findElement(By.xpath("//h3[contains(text(), 'Live Server Status')]"));
            assertTrue(serverStatusHeader.isDisplayed(), "Live Server Status header is missing");

        } finally {
            // Teardown: Guarantee the browser instance is destroyed even if assertions fail
            if (driver != null) {
                driver.quit();
            }
        }
    }
}
