package vn.aeoc.crm.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.aeoc.base.exception.AppException;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.repository.auth.ModuleRepository;
import vn.aeoc.crm.repository.auth.ResourceRepository;
import vn.aeoc.entity.data.mysql.auth.Module;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class ModuleServiceTest {
    private ModuleRepository moduleRepository;
    private ResourceRepository resourceRepository;
    private ModuleService service;

    @BeforeEach
    void setUp() {
        moduleRepository = mock(ModuleRepository.class);
        resourceRepository = mock(ResourceRepository.class);
        service = new ModuleService(moduleRepository, resourceRepository, mock(QueryExecutor.class));
    }

    @Test
    void deleteIsRejectedWhenModuleHasResources() {
        when(resourceRepository.countResourcesByModuleId(3)).thenReturn(1L);

        assertThrows(AppException.class, () -> service.deleteModule(3));
        verify(moduleRepository, never()).delete(3);
    }

    @Test
    void duplicateCodeIsRejected() {
        Module module = new Module();
        module.setCode("SALES");
        when(moduleRepository.existsCodeExceptId("SALES", null)).thenReturn(true);

        assertThrows(AppException.class, () -> service.upsert(module));
        verify(moduleRepository, never()).insertReturning(module);
    }

    @Test
    void duplicateCodeIsRejectedWhenUpdatingAnotherModule() {
        Module module = new Module();
        module.setId(2);
        module.setCode("SALES");
        when(moduleRepository.existsCodeExceptId("SALES", 2)).thenReturn(true);

        assertThrows(AppException.class, () -> service.upsert(module));
        verify(moduleRepository, never()).update(2, module);
    }

    @Test
    void assignsAllResourcesAfterValidatingIds() {
        when(moduleRepository.existsById(4)).thenReturn(true);
        when(resourceRepository.countResourcesByIds(List.of(10, 11))).thenReturn(2L);
        when(resourceRepository.assignResources(4, List.of(10, 11))).thenReturn(2);

        assertEquals(2, service.assignResources(4, List.of(10, 11, 10)));
        verify(resourceRepository).assignResources(4, List.of(10, 11));
    }

    @Test
    void invalidResourcePreventsAnyAssignment() {
        when(moduleRepository.existsById(4)).thenReturn(true);
        when(resourceRepository.countResourcesByIds(List.of(10, 999))).thenReturn(1L);

        assertThrows(AppException.class, () -> service.assignResources(4, List.of(10, 999)));
        verify(resourceRepository, never()).assignResources(anyInt(), anyList());
    }
}
