# Multibranch build strategy extension

## Purpose
This plugin provides addition configuration to prevent multi branch projects from triggering new builds
based on a include or exclude regions in source repository.



## Setup
Let's say you don't want your ci to run on README or .gitignore or any .html file change

![Multibranch build strategy extension](/images/plugin-options.png)

On multibranch job go to Build Strategy section , click add button and select
Cancel build excluded regions strategy 

![Multibranch build strategy extension](/images/exclude.png)

fill the textarea with ant style exclusions:
README.md
.gitignore
**/*.html

