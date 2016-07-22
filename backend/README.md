Phenotips HPO Request Backend
=============================

Contains the backend components for requesting new HPO terms.

Data Structures
---------------

#### `org.phenotips.termrequester.Phenotype`

Github interaction
------------------

#### `org.phenotips.termrequester.github.GithubAPIFactory`
##### `GithubAPI create(String owner, String repository, String oauthToken)`

#### `org.phenotips.termrequester.github.GithubAPI`
##### `createIssue(Phenotype pt)`
##### `readPhenotype(Phenotype pt)`
