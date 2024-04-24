# Instructions (Windows only)
# 1. Follow pre-requisite instructions in README.md
# 2. Update constants/code as needed
# 3. Run this from an IDE using F5 or with `python dmv_test.py` from a command line or double click the file
# 4. It will beep when it succeeds as configured by BEEP_DURATION_MS and BEEP_FREQUENCY_HZ

import time
import winsound
from selenium import webdriver
from selenium.webdriver.support.wait import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
import chromedriver_binary  # Adds chromedriver binary to path
from selenium.webdriver.common.by import By

BEEP_DURATION_MS = 10000
BEEP_FREQUENCY_HZ = 440
SCRIPT_FREQUENCY_S = 60
URL='https://coloradodrivinginstitute.as.me/Retest'

while True:
    driver = webdriver.Chrome()
    driver.get(URL)

    cal_select_btn = driver.find_element(By.XPATH, '//*[@id="select-calendar-options"]/div[2]/div[2]/div[1]/div/button')
    cal_select_btn.click()

    # wait 10 seconds for the page to load
    try:
        month_select_btn = WebDriverWait(driver, 10).until(
            EC.presence_of_element_located((By.CLASS_NAME, "react-calendar__navigation__label__labelText"))
        )
    except:
        driver.quit()

    print(f"Month selection button content: {month_select_btn.text}")

    # check whether date is available
    if 'April' in month_select_btn.text:
        print("Date is available!!!!!!!!!!!!!!")
        winsound.Beep(BEEP_FREQUENCY_HZ, BEEP_DURATION_MS)
        break

    time.sleep(SCRIPT_FREQUENCY_S)