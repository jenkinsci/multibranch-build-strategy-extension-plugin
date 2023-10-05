# Multibranch build strategy extension

## Purpose

This plugin provides additional configuration to prevent multi branch projects from triggering new builds
based on a include or exclude regions in source repository.  
Each region uses [ant pattern matching](https://ant.apache.org/manual/dirtasks.html), and must be separated by a new
line.  
Any commented line should start by **#**

### Examples

- 
    ```
     # excluding html and jpeg changes to trigger builds
     src/main/web/**/*.html
     src/main/web/**/*.jpeg
    ```

-
    ```
    # Any changes on java files will trigger build
    src/main/java/**/*.java
    ```

## Setup

Let's say you don't want your CI to run on `README.md`, `.gitignore` or any `.html` file changes

![Multibranch build strategy extension](/images/strategy_selection.png "strategy selection")

1. On multibranch job go to _Build Strategies_ section, click **Add** button and select _Cancel build excluded regions
   strategy_

![Multibranch build strategy extension](/images/exclusion_field.png "strategy configuration")

2. Fill the textarea with ant style exclusions:
   ```
   README.md
   .gitignore
   **/*.html
   ```

This can also be achieved by excluding using a file directly in the repository containing the regions

![Multibranch build strategy extension](/images/exclusion_file.png "strategy configuration")
