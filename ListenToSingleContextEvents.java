package BiDiLogging;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WindowType;
import org.openqa.selenium.bidi.log.ConsoleLogEntry;
import org.openqa.selenium.bidi.log.LogLevel;
import org.openqa.selenium.bidi.module.LogInspector;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class ListenToSingleContextEvents {

	// Logger instance to log test execution details and errors
	private static final Logger LOGGER = Logger.getLogger(ListenToSingleContextEvents.class.getName());

	// Constants used for test configuration
	private static final String WEB_PAGE_URL = "https://devtools.glitch.me/console/log.html"; // Actual URL stays intact
	private static final String ELEMENT_ID = "hello"; // ID of the element to be clicked
	private static final long TIMEOUT_SECONDS = 5; // Timeout duration for waiting for log entries

	private WebDriver driver; // WebDriver instance to interact with the browser
	private LogInspector logInspector; // LogInspector instance to capture console logs from the browser
	private String mainTab; // Handle for the main tab
	private String secondaryTab; // Handle for the secondary tab

	// This method runs before each test method to set up the environment
	@BeforeTest
	public void setup() {
		// Suppress the warning about CDP support for Firefox
		Logger.getLogger("org.openqa.selenium.firefox.FirefoxDriver").setLevel(Level.OFF);

		LOGGER.setLevel(Level.FINE);
		Arrays.stream(Logger.getLogger("").getHandlers()).forEach(handler -> handler.setLevel(Level.FINE));

		LOGGER.info("Initializing WebDriver with BiDi support.");

		FirefoxOptions options = new FirefoxOptions();
		options.enableBiDi();
		driver = new FirefoxDriver(options);

		mainTab = driver.getWindowHandle();
		LOGGER.info("Main tab handle assigned: " + mainTab);
	}

	// This method runs after each test method to clean up the environment
	@AfterTest
	public void teardown() {
		LOGGER.info("Tearing down WebDriver and LogInspector.");
		try {
			// Closing the LogInspector if it is open to release resources
			if (logInspector != null) {
				logInspector.close();
			}
		} catch (Exception e) {
			// Log any issues encountered while closing the LogInspector
			LOGGER.warning("Error closing LogInspector: " + e.getMessage());
		}

		// Quit the WebDriver session after the test completes
		if (driver != null) {
			driver.quit();
		}
	}

	// Test method to demonstrate subscribing to console logs from a specific tab
	@Test
	public void subscribeToSingleContextTest() throws InterruptedException, ExecutionException, TimeoutException {
		LOGGER.info("Starting subscribeToSingleContextTest.");

		// Step 1: Open a new tab and capture its window handle
		driver.switchTo().newWindow(WindowType.TAB); // Create a new tab
		secondaryTab = driver.getWindowHandle(); // Store the handle of the new tab
		LOGGER.info("Secondary tab handle assigned: " + secondaryTab); // Log the handle of the new tab

		// Step 2: Set up the LogInspector to listen only to the console logs of the
		// main tab
		logInspector = new LogInspector(mainTab, driver);

		// Step 3: Set up a CompletableFuture to asynchronously capture console log
		// entries
		CompletableFuture<ConsoleLogEntry> future = new CompletableFuture<>();
		// Attach the callback function to complete the future when a console log entry
		// is captured
		logInspector.onConsoleEntry(future::complete);

		// Step 4: Perform actions in the secondary tab (open a URL and click an
		// element)
		navigateToWebPageAndClick(secondaryTab, WEB_PAGE_URL, ELEMENT_ID);

		// Adding a brief pause (for demonstration purposes) to emphasize switching back
		// to the main tab
		Thread.sleep(3000); // 3-second pause to simulate real-world switching delay

		// Step 5: Switch back to the main tab and perform the same actions (open URL,
		// click element)
		navigateToWebPageAndClick(mainTab, WEB_PAGE_URL, ELEMENT_ID);

		// Step 6: Wait for the console log entry and verify the captured log entry
		try {
			// Attempt to get the console log entry from the future within the specified
			// timeout
			ConsoleLogEntry logEntry = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			LOGGER.fine("Verifying console log entry: " + logEntry); // Log the captured log entry

			// Assert that the console log entry contains the expected values
			Assert.assertEquals(logEntry.getText(), "Hello, Console!", "Unexpected log text.");
			Assert.assertEquals(logEntry.getType(), "console", "Unexpected log type.");
			Assert.assertEquals(logEntry.getLevel(), LogLevel.INFO, "Unexpected log level.");
		} catch (TimeoutException e) {
			// If the timeout is reached without capturing a log entry, log the error and
			// fail the test
			LOGGER.severe("Timeout waiting for console log entry: " + e.getMessage());
			Assert.fail("Test failed due to timeout.");
		}
	}

	/**
	 * Helper method to navigate to a webpage and click on a specified element by
	 * its ID. It switches to the provided tab, performs the actions, and logs the
	 * actions.
	 *
	 * @param tabHandle The tab handle to switch to (either mainTab or
	 *                  secondaryTab).
	 * @param url       The URL to navigate to in the browser.
	 * @param elementId The ID of the element to click on once the page is loaded.
	 */
	private void navigateToWebPageAndClick(String tabHandle, String url, String elementId) {
		// Switch to the specified tab and log the action
		driver.switchTo().window(tabHandle);

		// Log the navigation action (indicating which tab is used) with placeholder URL
		if (tabHandle.equals(mainTab)) {
			LOGGER.info("Navigating to WEB_PAGE_URL in main tab."); // Log with WEB_PAGE_URL placeholder
		} else {
			LOGGER.info("Navigating to WEB_PAGE_URL in secondary tab."); // Log with WEB_PAGE_URL placeholder
		}

		// Perform the navigation to the given URL
		driver.get(url);

		// Find the element by ID and click on it
		driver.findElement(By.id(elementId)).click();

		// Log the click action with dynamic tab reference
		if (tabHandle.equals(mainTab)) {
			LOGGER.info("Clicked element in main tab.");
		} else {
			LOGGER.info("Clicked element in secondary tab.");
		}
	}
}
