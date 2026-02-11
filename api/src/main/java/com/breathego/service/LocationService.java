package com.breathego.service;

import com.breathego.domain.Location;
import com.breathego.dto.LocationRequest;
import com.breathego.dto.LocationResponse;
import com.breathego.repository.LocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public List<LocationResponse> getAllLocations() {
        return locationRepository.findAll().stream()
                .map(LocationResponse::from)
                .toList();
    }

    public LocationResponse getLocation(Long id) {
        return locationRepository.findById(id)
                .map(LocationResponse::from)
                .orElseThrow(() -> new LocationNotFoundException(id));
    }

    public Location getLocationEntity(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new LocationNotFoundException(id));
    }

    public LocationResponse createLocation(LocationRequest request) {
        Location location = new Location(
                request.name(),
                request.latitude(),
                request.longitude()
        );

        if (request.timezone() != null) {
            location.setTimezone(request.timezone());
        }

        Location saved = locationRepository.save(location);
        return LocationResponse.from(saved);
    }

    public void deleteLocation(Long id) {
        if (!locationRepository.existsById(id)) {
            throw new LocationNotFoundException(id);
        }
        locationRepository.deleteById(id);
    }

    public static class LocationNotFoundException extends RuntimeException {
        public LocationNotFoundException(Long id) {
            super("Location not found with id: " + id);
        }
    }
}
