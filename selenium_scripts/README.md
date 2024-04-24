# Selenium Scripts Pre-requisites

1. Update Chrome from _chrome://settings/help_
1. `pip install selenium`
1. `pip install chromedriver-binary-auto`
   1. The webdriver must match the version of Chrome installed locally, and this package automatically downloads and adds the correct web driver to the $PATH. All scripts should now pick up the correct webdriver with `driver = webdriver.Chrome()`.
   1. If this becomes outdated, re-install in order to re-add the correct webdriver to the $PATH: `pip install --upgrade --force-reinstall chromedriver-binary-auto`
