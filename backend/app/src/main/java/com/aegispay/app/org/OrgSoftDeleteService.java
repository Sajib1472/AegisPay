package com.aegispay.app.org;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Soft-delete person and location only. Punches and earnings are never deleted.
 */
@Service
public class OrgSoftDeleteService {

    private final LocationRepository locations;
    private final PersonRepository people;

    public OrgSoftDeleteService(LocationRepository locations, PersonRepository people) {
        this.locations = locations;
        this.people = people;
    }

    @Transactional
    public Location archiveLocation(UUID locationId) {
        Location location = locations.findById(locationId).orElseThrow();
        if (location.getDeletedAt() == null) {
            location.setDeletedAt(Instant.now());
        }
        return location;
    }

    @Transactional
    public Person archivePerson(UUID personId) {
        Person person = people.findById(personId).orElseThrow();
        if (person.getDeletedAt() == null) {
            person.setDeletedAt(Instant.now());
        }
        return person;
    }
}
