package com.kendi.pos.restotable;

import com.kendi.pos.restotable.TableDtos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class RestaurantTableService {

    private final RestaurantTableRepository repository;

    public RestaurantTableService(RestaurantTableRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TableResponse> findAll(Section section) {
        List<RestaurantTable> tables = (section != null)
                ? repository.findAllBySection(section)
                : repository.findAll();
        return tables.stream().map(TableResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TableResponse findById(Long id) {
        RestaurantTable table = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found: " + id));
        return TableResponse.from(table);
    }

    public TableResponse create(CreateTableRequest request) {
        if (repository.existsByName(request.name())) {
            throw new RuntimeException("Table name already exists: " + request.name());
        }
        RestaurantTable table = new RestaurantTable();
        table.setName(request.name());
        table.setSeatCount(request.seatCount());
        table.setSection(request.section());
        table.setStatus(TableStatus.AVAILABLE);

        // Auto-set sortOrder nese s'jepet
        if (request.sortOrder() != null) {
            table.setSortOrder(request.sortOrder());
        } else {
            Integer maxOrder = repository.findAll().stream()
                    .map(RestaurantTable::getSortOrder)
                    .max(Integer::compareTo)
                    .orElse(0);
            table.setSortOrder(maxOrder + 1);
        }

        return TableResponse.from(repository.save(table));
    }

    public TableResponse update(Long id, UpdateTableRequest request) {
        RestaurantTable table = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found: " + id));
        table.setName(request.name());
        if (request.seatCount() != null) table.setSeatCount(request.seatCount());
        if (request.section() != null) table.setSection(request.section());
        if (request.sortOrder() != null) table.setSortOrder(request.sortOrder());
        return TableResponse.from(repository.save(table));
    }

    public TableResponse updatePosition(Long id, UpdatePositionRequest request) {
        RestaurantTable table = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found: " + id));
        table.setPositionX(request.positionX());
        table.setPositionY(request.positionY());
        return TableResponse.from(repository.save(table));
    }

    public TableResponse updateSize(Long id, UpdateSizeRequest request) {
        RestaurantTable table = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found: " + id));
        table.setSize(request.size());
        return TableResponse.from(repository.save(table));
    }

    public TableResponse updateStatus(Long id, UpdateStatusRequest request) {
        RestaurantTable table = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Table not found: " + id));
        table.setStatus(request.status());
        return TableResponse.from(repository.save(table));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Table not found: " + id);
        }
        repository.deleteById(id);
    }
}