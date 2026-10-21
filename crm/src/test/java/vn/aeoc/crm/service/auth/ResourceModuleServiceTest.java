package vn.aeoc.crm.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.repository.auth.ModuleRepository;
import vn.aeoc.crm.repository.auth.ResourceRepository;
import vn.aeoc.entity.data.mysql.auth.Resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ResourceModuleServiceTest {
    private ResourceRepository resourceRepository;
    private ModuleRepository moduleRepository;
    private ResourceService service;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(ResourceRepository.class);
        moduleRepository = mock(ModuleRepository.class);
        service = new ResourceService(resourceRepository, moduleRepository, mock(QueryExecutor.class));
        when(moduleRepository.existsById(5)).thenReturn(true);
    }

    @Test
    void insertKeepsModuleIdWhenNullOrSet() {
        Resource unassigned = new Resource();
        when(resourceRepository.insertResource(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(null, service.insert(unassigned).getModuleId());

        Resource assigned = new Resource();
        assigned.setModuleId(5);
        assertEquals(5, service.insert(assigned).getModuleId());
    }

    @Test
    void updateKeepsModuleIdWhenNullOrSet() {
        Resource unassigned = new Resource();
        when(resourceRepository.updateResource(any())).thenAnswer(invocation -> invocation.getArgument(0));
        assertEquals(null, service.update(7, unassigned).getModuleId());

        Resource assigned = new Resource();
        assigned.setModuleId(5);
        assertEquals(5, service.update(8, assigned).getModuleId());
        verify(resourceRepository, times(2)).updateResource(any());
    }
}
