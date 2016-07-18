Phenotips HPO TermRequester
===========================

This service is designed to request that new phenotypes be added to the HPO and give a handle
to requested phenotypes whilst they're being considered for inclusion.

It exposes the following REST api:

DATA STRUCTURES
---------------

### `phenotype`

```javascript
{
  'id': 'NONHPO_...',           /* The id of this phenotype within the request service */
  'name': '...',
  'status': { ... },            /* See spec for status object */
  'synonyms': ['...', '...', ],
  'description': '...',
  'issue_num': '...',           /* The github issue number in the HPO's github */
}
```

### `status`

One of:

- Submitted
```javascript
  {
    'status': 'SUBMITTED',
  }
```
- Rejected

```javascript
  {
    'status': 'REJECTED',
  }
```

- Accepted

```javascript
  {
    'status': 'ACCEPTED',
    'new_id': 'HPO_...',  /* The id of the created phenotype in the HPO */
  }
```

CREATE
------

### `POST /phenotypes/create`

Create a new phenotype request.
If one already exists with that same name or as a synonym of this one, none will be
created, and the previously existing one will be returned.

###### Parameters

```javascript
{
  'name': '...',
  'synonyms': ['...', '...', ],
  'description': '...',
}
```

###### Response

If the phenotype has already been requested, the response code will be `HTTP 409`.

In either case the new (or previously existing) phenotype will be returned
(see above for phenotype object format).

### `POST /phenotypes/force_create`

Create a new phenotype request, whether or not there's already an identical one.

###### Parameters

See `POST /phenotypes/create`

###### Response

Will always accept a (properly formatted) request and return the newly minted phenotype.


READ
----

### `GET /phenotypes/{id}`

###### Response

Returns the phenotype with the id given (see above for object format) or an empty `HTTP 404` if none exists.

### `GET /phenotypes/search`

###### Parameters

```javascript
{
  'text': '...',
}
```

###### Response

```javascript
{
  'metadata': {
    'text': '...',      /* Echo of the search text */
    'num_results': ..., /* Number of results returned */
  },
}
```
