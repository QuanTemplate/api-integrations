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