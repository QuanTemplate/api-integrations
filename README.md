# data-ingress

Integrations with third-party data providers such as [Capital IQ](https://www.capitaliq.com) leveraging the [Quantemplate Data Ingress API](https://quantemplate.readme.io/docs/getting-started#-data-ingress)

## Setup

1. Install [coursier](https://get-coursier.io/docs/cli-installation)


2. Install all necessary dev tools:
    ```
    cs setup
    ```


# Capital IQ

Capital IQ is a reservoir of financial data which could be accessed through the API.
We could access different information based on the passed `Mnemonic` token.
Each `Mnemonic` has access to at least one of `Functions`. `Function`s specify the type of the response. Each function in different `Mnemonic` has access to different `Property Type`.

## Capital IQ functions

- **GDSP** - Retrieves a single data point for a point in time
- **GDSPV** - Retrieves an array of values for the most current availability of content either end of day or intra-day
- **GDSG** - Retrieves a set of values that belong to a specific group using different mnemonics
- **GDSHE** - Retrieves historical values for a mnemonic over a range of dates
- **GDSHV** - Retrieves an array or set of values over a historical range of dates
- **GDST** - Retrieves historical values for a mnemonic over a range of dates with a specific frequency

## Available Mnemonics
- [**IQ_TOTAL_REV**](https://support.standardandpoors.com/gds/index.php?option=com_content&view=article&id=545671:total-revenues&catid=12468&Itemid=301)
- [**IQ_COMPANY_NAME_LONG**](https://support.standardandpoors.com/gds/index.php?option=com_content&view=article&id=554261:iq-company-name-long&catid=12646&Itemid=301)


## Development

1. create `api-client` user for a given env
    You can use the [`create-api-user`](https://github.com/QuanTemplate/scripts/tree/master/create-api-user)

2. Create `.env` file in the project root with the following variables:

    ```
    CAPITALIQ_API_USERNAME=<api username you got with Capital IQ API license>
    CAPITALIQ_API_PASSWORD=<password for the corresponding Capital IQ API user>
    QT_ENV=<name of the Quantemplate environment, should be `prod` for any consumers>
    QT_AUTH_REALM=<name of the Quantemplate auth ream, should be `qt` for any consumers>
    QT_CLIENT_ID=<id of the api-client user generated by the Quantemplate team>
    QT_CLIENT_SECRET=＜password for the used api-client user>
    ```

    for example, for the QT dev env:
    ```
    CAPITALIQ_API_USERNAME=apiadmin@quantemplate.com
    CAPITALIQ_API_PASSWORD=<password from dev credentials>
    QT_ENV=dev
    QT_AUTH_REALM=test
    QT_CLIENT_ID=<id of the api-client user>
    QT_CLIENT_SECRET=＜password for the used api-client user>
    ```

    and export it:

    ```sh
    export $(xargs < .env)
    ```

3. run the script

    ```sh
    sbt capitaliq/run
    ```

## Packaging

creating a fat jar
```
sbt capitaliq/assembly
```

## Running the program

**Prerequisites** - you need to have appropriate env variables set up, see the [dev guide](#development) for more info


### Generating a total revenue report from the CapitalIQ data and uploading it to the Quantemplate dataset

```sh
cat ./data/capitaliq-identifiers.txt | java -jar ./capitaliq/target/scala-3.0.0-RC1/capitaliq-assembly-1.0.jar generateRevenueReport --orgId c-my-small-insuranc-ltdzfd --datasetId d-e4tf3yyxerabcvicidv5oyey --currency USD --from 1988-12-31 --to 2018-12-31
```

