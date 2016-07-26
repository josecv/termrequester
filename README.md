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
  'status': '...', /* See the status below */
  'synonyms': ['...', '...', ],
  'description': '...',
  'issueNumber': '...',           /* The github issue number in the HPO's github */
  'parents': ['...', ], /* The ids of this phenotype's parents */
  'hpoId': '...', /* The id of the term in the HPO. Non-null iff status == ACCEPTED */
}
```

### `status`

One of:

- `SUBMITTED`
- `REJECTED`
- `ACCEPTED`

CREATE
------

### `POST /phenotypes/create`

Create a new phenotype request.
If one already exists with that same name or as a synonym of this one, none will be
created, and the previously existing one will be returned.

###### Parameters

These are an exact match of the phenotype data type, only missing the `status`, `id` and `issue_num`.

```javascript
{
  'name': '...',
  'synonyms': ['...', '...', ],
  'description': '...',
  'parents': ['...', '...', ],
}
```

###### Response

If the phenotype has already been requested, the response code will be `HTTP 409`.

In either case the new (or previously existing) phenotype will be returned
(see above for phenotype object format).

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
  'results': [ {...}, ... ], /* The results as phenotype instances */
}
```
