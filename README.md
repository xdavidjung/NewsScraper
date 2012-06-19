# NewsScraper

NewsScraper is a program that takes in raw news data from an RSS feed and
processes it using ReVerb, extracting binary relationships from "useful" 
sentences in the RSS feed articles. 

The YahooRssConfig file contains information about how the Yahoo RSS will
be parsed.


## Quick use:

usage: options:

    -fc <arg>   This option cannot be used without the fmt option.
                Specify the category name; if not specified, all categories
                will be used.

    -fct <arg>  This option cannot be used without the fmt option.
                Specify a minimum confidence requirement; if not specified,
                then a default number of extractions will be taken.

    -fd <arg>   This option cannot be used without the fmt option.
                Specify the directory of source files and a target file; if
                not specified, then a default will be used.

    -fmt        Format the reverb news database into human readable file.

    -ft <arg>   This option cannot be used without the fmt option.
                Specify the time interval. The files that fall into this
                interval will be formatted (for eg: 2012-05-01 2012-05-04); 
                if not specified, then a default interval will be formatted.

    -ftoday     Format today's file; this can not be used with the ft option.

    -h          Print program usage.

    -p <arg>    Process RSS only: the first arg is the source directory, the
                second arg is the target directory where data will be saved.

    -r          Use reverb to extract today's file.

    -rd <arg>   Use reverb to extract files in the first arg and save it
                into second arg directory.

    -s          Fetch the RSS (without processing it)

    -sp         Fetch the RSS and process it.


## Extraction Categories:

a: Perfect news, eg: Singer Nicki Minaj performs a free concert at  Times Square.

b: Existing general knowledge, eg: Obama is the president of United States.

c: One of the two arguments is uninformative, eg: This application is known for proper handling of global communication.

d: Both arguments are uninformative, eg: He likes his idea.

e: Extraction is fine, but meaningless, eg: A photo is still a photo.

f: Extraction has a different meaning from the original sentence, eg: the world's largest listed oil firm was the product of nearly a year of talks.


## Parsing Particulars

Leading publisher info (eg: "(Reuters) -" ) is thrown.

If the content ends with "./!/? ...", then "..." is thrown.

If the content ends with "[...]" then it is incomplete and consequently abandoned.

If the content ends with "..." then it is incomplete and consequently abandoned.


## Known Problems

After scraping the text from yahoo news, some of the tags are in wrong position.

eg: `<link />http://xxxx` instead of `<link>http://xxx</link>` 
or
`<source><source/>Reuters` instead of `<source>Reuters</source>`


## Abandoned Yahoo RSS Feeds

"Dear Abby" does not seem useful.

"All Politics" in the POLITICS category is the same as 
"Politics" in the US category; "All Politics" in POLITICS is picked.

"Theater" under ENTERTAINMENT category and "World" under POLITICS 
category are abandoned - those feeds are currently unavailable.


## Miscellaneous

When outputting data, the program doesn't use JSON's output method;
instead, the program will print the JSON string itself. This is because
the JSON library provided does not handle UTF-8 encoding well.