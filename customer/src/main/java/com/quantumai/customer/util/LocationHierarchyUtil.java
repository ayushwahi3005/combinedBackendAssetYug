package com.quantumai.customer.util;

import com.quantumai.customer.entity.Assets;
import com.quantumai.customer.entity.Bin;
import com.quantumai.customer.entity.Location;
import com.quantumai.customer.repository.BinRepository;
import com.quantumai.customer.repository.LocationRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LocationHierarchyUtil {

  @Autowired private LocationRepository locationRepository;
  @Autowired private BinRepository binRepository;

  public String resolveAssetLocationName(Assets asset) {
    if (asset == null || asset.getLocation() == null || asset.getLocation().isBlank()) {
      return "";
    }
    return resolveLocationReference(asset.getLocation());
  }

  public String resolveLocationReference(String locationRef) {
    if (locationRef == null || locationRef.isBlank()) {
      return "";
    }

    if (locationRef.startsWith("bin:")) {
      Optional<Bin> binOptional = binRepository.findById(locationRef.substring(4));
      if (binOptional.isEmpty()) {
        return locationRef;
      }
      Bin bin = binOptional.get();
      if (bin.getLocationId() != null) {
        String hierarchyPath = buildLocationHierarchyPath(bin.getLocationId());
        return hierarchyPath + " -> " + nullToEmpty(bin.getBinNumber());
      }
      return nullToEmpty(bin.getBinNumber());
    }

    if (locationRef.startsWith("location:")) {
      return locationRepository
          .findById(locationRef.substring(9))
          .map(this::buildLocationHierarchyPath)
          .orElse(locationRef);
    }

    if (locationRef.startsWith("bin")) {
      Optional<Bin> binOptional = binRepository.findById(locationRef.substring(4));
      if (binOptional.isEmpty()) {
        return locationRef;
      }
      Bin bin = binOptional.get();
      if (bin.getLocationId() != null) {
        String hierarchyPath = buildLocationHierarchyPath(bin.getLocationId());
        return hierarchyPath + " -> " + nullToEmpty(bin.getBinNumber());
      }
      return nullToEmpty(bin.getBinNumber());
    }

    if (locationRef.startsWith("location")) {
      return locationRepository
          .findById(locationRef.substring(9))
          .map(this::buildLocationHierarchyPath)
          .orElse(locationRef);
    }

    return locationRef;
  }

  public String buildLocationHierarchyPath(Location location) {
    return buildLocationHierarchyPath(location, new HashSet<>());
  }

  private String buildLocationHierarchyPath(Location location, Set<String> visited) {
    if (location == null) {
      return "";
    }

    String locationId = location.getId();
    if (locationId != null && !visited.add(locationId)) {
      log.warn("Circular location hierarchy detected for location id: {}", locationId);
      return nullToEmpty(location.getName());
    }

    String parentLocationId = location.getParentLocation();
    if (parentLocationId == null
        || parentLocationId.isBlank()
        || parentLocationId.equals(locationId)) {
      return nullToEmpty(location.getName());
    }

    Optional<Location> parentLocationOptional = locationRepository.findById(parentLocationId);
    if (parentLocationOptional.isPresent()) {
      String parentPath = buildLocationHierarchyPath(parentLocationOptional.get(), visited);
      return parentPath + " -> " + nullToEmpty(location.getName());
    }

    return nullToEmpty(location.getName());
  }

  private String nullToEmpty(String value) {
    return value != null ? value : "";
  }
}
