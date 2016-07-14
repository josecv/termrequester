Phenotips HPO Request Backend
=============================

Contains the backend components for requesting new HPO terms.

Data Structures
---------------

#### `org.phenotips.hporequest.Phenotype`

Github interaction
------------------

#### `org.phenotips.hporequest.github.GithubAPIFactory`
##### `GithubAPI create(String repository)`

#### `org.phenotips.hporequest.github.GithubAPI`
##### `createIssue(Phenotype pt)`
##### `getStatus(Phenotype pt)`
