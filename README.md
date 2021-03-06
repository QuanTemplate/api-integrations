# api-integrations

Integrations with third-party data providers such as [Capital IQ](https://www.capitaliq.com) or [Google](https://developers.google.com/maps/documentation), leveraging the [Quantemplate API](https://quantemplate.readme.io/docs/getting-started).

Learn more at https://quantemplate.readme.io/

If you need help please contact us at support@quantemplate.com

## Running the program

**Prerequisites** 
- you need to have appropriate env variables set up, see the [dev guide](#development) for more info
- you need to build the app first or use an already build package
    - see the [releases](https://github.com/QuanTemplate/api-integrations/releases) to download the jar
    - see the [packaging](#packaging) for how to build the jar from sources

The integration program offers a bunch of commands, consult the `Use Cases` sections in the integrations listed below.

# Integrations

- # S&P Capital IQ

    Capital IQ is a reservoir of financial data which could be accessed through the API and used in the Quantemplate.



    ## Use Cases

    - [Generating a total revenue report from the Capital IQ data and uploading it to the Quantemplate dataset](https://quantemplate.readme.io/docs/example-capital-iq-integration)

        - with yaml config:
            ```sh
            java -jar ./integrations/target/scala-3.0.0/qt-integrations-0.1.4.jar apply ./data/revReport.yml
            ```

            Check out the [config file](./data/revReport.yml)


        - with CLI args:
            ```sh
            cat ./data/capitaliq-identifiers.txt | java -jar ./integrations/target/scala-3.0.0/qt-integrations-0.1.4.jar generateRevenueReport --orgId c-my-small-insuranc-ltdzfd --datasetId d-e4tf3yyxerabcvicidv5oyey --currency USD --from 1988-12-31 --to 2018-12-31
            ```

    - [Generating a multi-data point report for a single date with multiple Capital IQ mnemonics and uploading it to the Quantemplate dataset](https://quantemplate.readme.io/docs/example-capital-iq-integration-2)

        - with yaml config:
            ```sh
            java -jar ./integrations/target/scala-3.0.0/qt-integrations-0.1.4.jar apply ./data/multiPointReport.yml
            ```

            Check out the [config file](./data/multiPointReport.yml)


    ### Capital IQ request definition

    We could access different information based on the passed `Mnemonic` token.
    Each `Mnemonic` has access to at least one of `Functions`. `Function`s specify the type of the response. Each function in different `Mnemonic` has access to different `Property Type`.

    Each request requires a CapitalIQ identifier which must be known beforehand.
    An example set of identifiers could be found in the `./data/capitaliq-identifiers.txt`

    #### Capital IQ functions

    - **GDSP** - Retrieves a single data point for a point in time
    - **GDSPV** - Retrieves an array of values for the most current availability of content either end of day or intra-day
    - **GDSG** - Retrieves a set of values that belong to a specific group using different mnemonics
    - **GDSHE** - Retrieves historical values for a mnemonic over a range of dates
    - **GDSHV** - Retrieves an array or set of values over a historical range of dates
    - **GDST** - Retrieves historical values for a mnemonic over a range of dates with a specific frequency

    #### Available Mnemonics

    Check out the sources of [CapitalIQ.Mnemonic](./integrations/src/main/scala/com/quantemplate/integrations/capitaliq/CapitalIQ.scala)

- # Google Geocoding Service

    Google Geocoding API offers a way of converting human-readable addresses to geographic coordinates - latitude and longitude, and fetch additional information about given locations.


    ## Use cases
    - [Automated pipeline execution with address cleansing](https://quantemplate.readme.io/docs/example-google-geocoding-integration)
        - with yaml config:
            ```sh
            java -jar ./integrations/target/scala-3.0.0/qt-integrations-0.1.4.jar apply ./data/addressCleanse.yml
            ```

# Development 

## Environment setup

1. Install [coursier](https://get-coursier.io/docs/cli-installation)


2. Install all necessary dev tools:
    ```
    cs setup
    ```
## Config

1. create `api-client` user for a given env

    You can use the [`create-api-user`](https://github.com/QuanTemplate/scripts/tree/master/create-api-user) script.
    If you are not member of the Quantemplate team, then please reach out to us at support@quantemplate.com

2. Create `.env` file in the project root with the following variables:
    
    You can omit API keys and credentials for any third-party services you won't be using.
    
    ```
    CAPITALIQ_API_USERNAME=<api username you got with Capital IQ API license>
    CAPITALIQ_API_PASSWORD=<password for the corresponding Capital IQ API user>
    CAPITALIQ_DEMO_ACCOUNT=<indicates whether the Capital IQ demo account is used>
    QT_ENV=<name of the Quantemplate environment, should be `prod` for any consumers>
    QT_AUTH_REALM=<name of the Quantemplate auth ream, should be `qt` for any consumers>
    QT_CLIENT_ID=<id of the api-client user generated by the Quantemplate team>
    QT_CLIENT_SECRET=???password for the used api-client user>
    GOOGLE_MAPS_API_KEY=<api key for google maps>
    ```

    for example, for the QT dev env:
    ```
    CAPITALIQ_API_USERNAME=apiadmin@quantemplate.com
    CAPITALIQ_API_PASSWORD=<password from dev credentials>
    CAPITALIQ_DEMO_ACCOUNT=true
    QT_ENV=dev
    QT_AUTH_REALM=test
    QT_CLIENT_ID=<id of the api-client user>
    QT_CLIENT_SECRET=???password for the used api-client user>
    GOOGLE_MAPS_API_KEY=<api key for google maps>
    ```

    and export it:

    ```sh
    export $(xargs < .env)
    ```

3. run the script

    ```sh
    sbt integrations/run
    ```

## Packaging

creating a fat jar
```
sbt integrations/assembly
```

## Testing

```
sbt integrations/test
```
