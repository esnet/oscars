@ProjectIdSteps
Feature: Validation of project ids

  Scenario: Project-ids optional, always succeeds
    When I set the project id validation mode to "optional"
    Then I did not receive an exception

    When I load a SimpleConnection from "simple-conns/projectids-missing.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation succeeded

    When I load a SimpleConnection from "simple-conns/projectids-empty.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation succeeded

    When I load a SimpleConnection from "simple-conns/projectids-empty-strings-inside.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation succeeded

    When I load a SimpleConnection from "simple-conns/projectids-not-in-esdb.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation succeeded

    When I load a SimpleConnection from "simple-conns/projectids-in-esdb-no-orcid-user.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation succeeded

    When I load a SimpleConnection from "simple-conns/projectids-in-esdb-with-orcid-user.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation succeeded


  Scenario: Project-ids mandatory, no ESDB validation. Fails on missing / empty only; succeeds if any projectids present
    When I set the project id validation mode to "mandatory"
    Then I did not receive an exception

    When I load a SimpleConnection from "simple-conns/projectids-missing.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation failed

    When I load a SimpleConnection from "simple-conns/projectids-empty.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation failed

    When I load a SimpleConnection from "simple-conns/projectids-empty-strings-inside.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation failed

    When I load a SimpleConnection from "simple-conns/projectids-not-in-esdb.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation succeeded

    When I load a SimpleConnection from "simple-conns/projectids-in-esdb-no-orcid-user.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation succeeded

    When I load a SimpleConnection from "simple-conns/projectids-in-esdb-with-orcid-user.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation succeeded




  Scenario: Project-ids must be in ESDB with the "Project" org-type.
    When I set the project id validation mode to "in-esdb"
    Then I did not receive an exception

    When I load a SimpleConnection from "simple-conns/projectids-missing.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation failed

    When I load a SimpleConnection from "simple-conns/projectids-empty.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation failed

    When I load a SimpleConnection from "simple-conns/projectids-empty-strings-inside.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation failed

    When I load a SimpleConnection from "simple-conns/projectids-not-in-esdb.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation failed

#  DOES succeed if project does NOT have a contact with a valid orc-id
    When I load a SimpleConnection from "simple-conns/projectids-in-esdb-no-orcid-user.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation succeeded

#  And also if project does in fact have a contact with a valid orc-id
    When I load a SimpleConnection from "simple-conns/projectids-in-esdb-with-orcid-user.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation succeeded



  Scenario: Project-ids must be in ESDB and have a valid orc-id user
# Does NOT succeed if project does NOT have a contact with a valid orc-id
    When I set the project id validation mode to "user-with-orcid"
    Then I did not receive an exception

    When I load a SimpleConnection from "simple-conns/projectids-missing.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation failed

    When I load a SimpleConnection from "simple-conns/projectids-empty.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation failed

    When I load a SimpleConnection from "simple-conns/projectids-empty-strings-inside.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation failed

    When I load a SimpleConnection from "simple-conns/projectids-not-in-esdb.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation failed

#  DOES NOT succeed if project does NOT have a contact with a valid orc-id
    When I load a SimpleConnection from "simple-conns/projectids-in-esdb-no-orcid-user.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation failed

#  Only succeeds when project DOES have a contact with a valid orc-id
    When I load a SimpleConnection from "simple-conns/projectids-in-esdb-with-orcid-user.json"
    When I validate projectIds for the SimpleConnection
    Then the projectIds validation succeeded


  Scenario: Validate orcid format
    # known good orcids from https://support.orcid.org/hc/en-us/articles/360006897674-Structure-of-the-ORCID-Identifier
    When I perform orcid validation on "https://orcid.org/0000-0002-1825-0097"
    Then the orcid validation succeeded

    When I perform orcid validation on "https://orcid.org/0000-0001-5109-3700"
    Then the orcid validation succeeded

    When I perform orcid validation on "https://orcid.org/0000-0002-1694-233X"
    Then the orcid validation succeeded

    # invalid check digits
    When I perform orcid validation on "https://orcid.org/0000-0002-1694-2331"
    Then the orcid validation failed

    When I perform orcid validation on "https://orcid.org/0000-0001-5109-3709"
    Then the orcid validation failed

    # other invalid formatting
    When I perform orcid validation on "https://orcid.org/0000-0001-1234"
    Then the orcid validation failed

    When I perform orcid validation on ""
    Then the orcid validation failed

    When I perform orcid validation on "junk-data"
    Then the orcid validation failed