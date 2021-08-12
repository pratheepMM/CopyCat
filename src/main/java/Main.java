import com.google.common.base.Function;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;

import java.io.*;
import java.time.Duration;
import java.util.HashMap;

import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Main {

    static Wait<WebDriver> wait ;
    static WebDriver driver;
    static String cookieFileName = "Credentials/.cookies.ser";
    static String defaultSite = "https://google.com";
    static String credentialsFileName = "/home/pratheep/Projects/IdeaProjects/CopyCat/Credentials/credentials.txt";
    static String driverLocation = "/home/pratheep/Projects/IdeaProjects/CopyCat/Driver/chromedriver";
    static String browserBinary = "/usr/bin/brave-browser";

    static void setDriver(){
        System.setProperty("webdriver.chrome.driver",driverLocation);
        ChromeOptions options = new ChromeOptions().setBinary(browserBinary);
        driver =  new ChromeDriver(options);
        wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(5))
                .pollingEvery(Duration.ofSeconds(1))
                .ignoring(NoSuchElementException.class);
    }

    static String[] getCredentials() throws Exception{
        String[] credentials = new String[2];
        File fileName = new File(credentialsFileName);
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        credentials[0] = br.readLine();
        credentials[1] = br.readLine();
        br.close();
        return credentials;
    }

    static void login(String[] credentials){
        String mailID = credentials[0] , password = credentials[1];
        WebElement signInButton = wait.until((Function<WebDriver, WebElement>) driver -> {
            WebElement element = driver.findElement(By.xpath("//a[normalize-space()='Sign in']"));
            if (element.isDisplayed()) {
                return element;
            } else {
                return null;
            }
        });
        signInButton.click();
        WebElement emailField = wait.until((Function<WebDriver, WebElement>) driver -> {
            WebElement element = driver.findElement(By.xpath("//input[@id='identifierId']"));
            if (element.isDisplayed()) {
                return element;
            } else {
                return null;
            }
        });
        emailField.sendKeys(Keys.chord(mailID,Keys.ENTER));
        WebElement passwordField = wait.until(new Function<WebDriver,WebElement>(){
            public WebElement apply(WebDriver driver){
                WebElement element = driver.findElement(By.xpath("//input[@name='password']"));
                if(element.isDisplayed()){
                    return element;
                }
                else{
                    return null;
                }
            }
        });
        passwordField.sendKeys(Keys.chord(password,Keys.ENTER));
    }

    static void loadFromPage(String from) throws Exception {
        Set<Cookie> cookies = getCookies();
        driver.get(defaultSite);
        if(cookies == null){
            String[] credentials = getCredentials();
            login(credentials);
            Thread.sleep(5000);
            saveCookies();
            driver.get(from);
        }
        else{
            loadCookies(cookies);
            driver.get(from);
        }
    }

    static Set<Cookie> getCookies() {
        try {
            FileInputStream fileInputStream = new FileInputStream(cookieFileName);
            ObjectInputStream inputStream = new ObjectInputStream(fileInputStream);
            return (Set<Cookie>) inputStream.readObject();
        }
        catch(Exception e){
            return null;
        }

    }

    static void saveCookies() throws Exception{
        Set<Cookie> cookies = driver.manage().getCookies();
        File file = new File(cookieFileName);
        if(file.exists()){
            file.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream);
        outputStream.writeObject(cookies);
    }

    static void loadCookies(Set<Cookie> cookies){
        try {
            driver.manage().deleteAllCookies();
            for (Cookie cookie : cookies) {
                driver.manage().addCookie(cookie);
            }
        }
        catch(Exception ignored){
        }
    }

    static HashMap<String,String> getContents(){
        HashMap<String,String> map = new HashMap<>();
        List<WebElement> list = wait.until((Function<WebDriver, List<WebElement>>) driver -> {
            List<WebElement> list1 = driver.findElements(By.className("freebirdFormviewerViewNumberedItemContainer"));
            if(list1.size()==0){
                return null;
            }
            return list1;
        });
        String questionCssSelector = "div[class='freebirdFormviewerViewItemsItemItemTitle exportItemTitle freebirdCustomFont']";
        String optionsClassSelector = "freebirdFormviewerViewItemsRadioOptionContainer";
        String iconClassSelector = "freebirdFormviewerViewItemsItemChoiceCorrectnessIcon";
        String answerCssSelector = "span[class='docssharedWizToggleLabeledLabelText exportLabel freebirdFormviewerViewItemsRadioLabel']";

        for(WebElement element : list){
            try {
                WebElement questionElement = element.findElement(By.cssSelector(questionCssSelector));
                String question = questionElement.getText();
                List<WebElement> optionsElements = element.findElements(By.className(optionsClassSelector));
                boolean is = false;
                for(WebElement option : optionsElements){
                    try {
                        WebElement icon = option.findElement(By.className(iconClassSelector));
                        if(icon.getAttribute("aria-label").equals("Correct")){
                            String answer = option.findElement(By.cssSelector(answerCssSelector)).getText();
                            map.put(question,answer);
                            is = true;
                            break;
                        }
                    }
                    catch(Exception ignored){

                    }
                }
                if(!is){
                    try{
                        String outerCorrectAnswerCssSelector = "div[class='freebirdFormviewerViewItemsItemGradingCorrectAnswerBox']";
                        WebElement outerCorrectAnswer = element.findElement(By.cssSelector(outerCorrectAnswerCssSelector));
                        String correctAnswerCssSelector = "span[class='docssharedWizToggleLabeledLabelText exportLabel freebirdFormviewerViewItemsRadioLabel']";
                        String answer = outerCorrectAnswer.findElement(By.cssSelector(correctAnswerCssSelector)).getText();
                        map.put(question,answer);
                    }
                    catch(Exception ignored){}
                }
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
        return map;
    }

    static void putAnswers(String to,HashMap<String,String> map){
        driver.get(to);
        List<WebElement> list = wait.until((Function<WebDriver, List<WebElement>>) driver -> {
            List<WebElement> list1 = driver.findElements(By.className("freebirdFormviewerViewNumberedItemContainer"));
            if (list1.size() == 0) {
                return null;
            }
            return list1;
        });
        String questionCssSelector = "div[class='freebirdFormviewerComponentsQuestionBaseTitle exportItemTitle freebirdCustomFont']";
        String optionsCssSelector = "div[class='freebirdFormviewerComponentsQuestionRadioChoice freebirdFormviewerComponentsQuestionRadioOptionContainer']";
        for(WebElement element : list){
            try {
                String question = element.findElement(By.cssSelector(questionCssSelector)).getText();
                String answer = map.get(question);
                if (answer == null)
                    continue;
                List<WebElement> options = element.findElements(By.cssSelector(optionsCssSelector));
                for(WebElement option : options){
                    try {
                        String optionValueCssSelector = "span[class='docssharedWizToggleLabeledLabelText exportLabel freebirdFormviewerComponentsQuestionRadioLabel']";
                        WebElement optionElement = option.findElement(By.cssSelector(optionValueCssSelector));
                        String optionValue = optionElement.getText();
                        if (optionValue.equals(answer)) {
                            WebElement parent = option.findElement(By.xpath("./.")).findElement(By.cssSelector("div[class='appsMaterialWizToggleRadiogroupElContainer exportContainerEl  docssharedWizToggleLabeledControl freebirdThemedRadio freebirdThemedRadioDarkerDisabled']"));
                            parent.click();
                            break;
                        }
                    }
                    catch (Exception ignore){}
                }
            }catch(Exception ignore){}
        }
    }

    static void submit(){
        driver.findElement(By.xpath("//span[contains(text(),'Submit')]")).click();
    }


    public static void main(String[] args) throws Exception{
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the response URL of a google form : ");
        String from = sc.nextLine().strip();
        System.out.println("Enter the new URL for the google form : ");
        String to = sc.nextLine().strip();
        setDriver();
        loadFromPage(from);
        HashMap<String,String> answers = getContents();
        putAnswers(to,answers);
        submit();
    }
}

