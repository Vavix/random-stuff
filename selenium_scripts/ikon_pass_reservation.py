# INSTRUCTIONS
# 1. Follow pre-requisite instructions in README.md
# 2. Create a file called credentials.txt with Ikon email on the first line and password on the second
# 3. Adjust to the date you desire and frequency of script below
# 4. Make sure the resort you want is favorited and is the only favorite
# 5. To run the script, either run `python ikon_pass_reservation.py` in CMD or just double click it
# 6. You know that it works when it opens a new Chrome window
# 7. ???
# 8. PROFIT!

import time
import winsound
import chromedriver_binary  # Adds chromedriver binary to path
from selenium import webdriver
from selenium.webdriver.common.keys import Keys
from selenium.webdriver.common.by import By
from selenium.webdriver.support.wait import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.remote.webelement import WebElement

BEEP_DURATION_MS = 10000
BEEP_FREQUENCY_HZ = 440
SCRIPT_FREQUENCY_S = 60
DATE = 'Sun Feb 14 2021' # must be exact or script will fail

with open('credentials.txt', 'r') as file:
    email_cred = file.readline().replace('\n', '')
    pw_cred = file.readline().replace('\n', '')

while True:
    driver = webdriver.Chrome()
    driver.get("https://account.ikonpass.com/en/login?redirect_uri=/en/myaccount/add-reservations/")

    email = driver.find_element_by_id("email")
    email.send_keys(email_cred)

    password = driver.find_element_by_id("sign-in-password")
    password.send_keys(pw_cred)
    password.send_keys(Keys.RETURN)

    # wait for page to load
    try:
        root = selection_page = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.ID, "root"))
        )
    except:
        driver.quit()
    time.sleep(3)

    # resort selection
    text = root.find_element_by_class_name('react-autosuggest__input')
    text.send_keys(Keys.DOWN)
    text.send_keys(Keys.ENTER)
    time.sleep(1)

    # click "Continue"
    continue_button = root.find_element_by_xpath('//div/div/main/section[2]/div/div[2]/div[2]/div[2]/button')
    continue_button.click()
    time.sleep(1.5)

    # check whether date is available
    desired_date:WebElement = root.find_element_by_xpath(f"//div[@aria-label='{DATE}']")
    if 'DayPicker-Day--unavailable' not in desired_date.get_attribute('class'):
        print("Date is available!!!!!!!!!!!!!!")
        winsound.Beep(BEEP_FREQUENCY_HZ, BEEP_DURATION_MS)
        break
    else:
        driver.quit()

    # delay before trying again - if your IP address gets banned, don't blame me
    time.sleep(SCRIPT_FREQUENCY_S)