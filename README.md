## data-ingress

Integrations with third-party data providers such as [Capital IQ](https://www.capitaliq.com) leveraging the [Quantemplate Data Ingress API](https://quantemplate.readme.io/docs/getting-started#-data-ingress)

## development

1. Create `.env` file in the project root with the following variables:

```
CAPITALIQ_API_USERNAME=<username>
CAPITALIQ_API_PASSWORD=<password>
```

and export it:

```
export $(xargs < .env)
```

2. run the script(s)

```
sbt run
```

## Capital IQ

Capital IQ is a reservoir of financial data which could be accessed through the API.
We could access different information based on the passed `Mnemonic` token.
Each `Mnemonic` has access to at least one of `Functions`. `Function`s specify the type of the response. Each function in different `Mnemonic` has access to different `Property Type`

## Available functions

- **GDSP** Retrieves a single data point for a point in time */
- **GDSPV** Retrieves an array of values for the most current availability of content either end of day or intra-day
- **GDSG** Retrieves a set of values that belong to a specific group using different mnemonics
- **GDSHE** Retrieves historical values for a mnemonic over a range of dates
- **GDSHV** Retrieves an array or set of values over a historical range of dates
- **GDST** Retrieves historical values for a mnemonic over a range of dates with a specific frequency

### todo:

- revenue spreadsheet:

  - [X] handle API errors
  - [X] split requests to capital IQ per limit, merge them together
  - [ ] transform to xlsx

  - [] handle ` (Data Unavailable,)`
  - [] merge request for company names
- QT integration:

  - send data to qt data repo
