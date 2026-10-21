package vn.aeoc.crm.service.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.aeoc.base.config.extention.paging.Page;
import vn.aeoc.base.config.extention.paging.Pageable;
import vn.aeoc.base.util.QueryExecutor;
import vn.aeoc.crm.repository.auth.ModuleRepository;
import vn.aeoc.crm.repository.auth.ResourceRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ResourceServiceTest {
    private ResourceRepository resourceRepository;
    private ResourceService service;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(ResourceRepository.class);
        service = new ResourceService(resourceRepository, mock(ModuleRepository.class), new QueryExecutor(Runnable::run));
    }

    @Test
    void unassignedFilterIsUsedForItemsAndTotalAlongsideKeywordAndActive() {
        Pageable pageable = new Pageable().setPage(2).setLimit(5);
        when(resourceRepository.getResourceByCriteria("reports", 1, null, true, pageable)).thenReturn(List.of());
        when(resourceRepository.countResourceByCriteria("reports", 1, null, true)).thenReturn(8L);

        Page<?> result = service.getListResourceByCriteria("reports", 1, null, true, pageable);

        assertEquals(8L, result.getTotal());
        assertEquals(2, result.getPage());
        assertEquals(8L, pageable.getTotal());
        verify(resourceRepository).getResourceByCriteria("reports", 1, null, true, pageable);
        verify(resourceRepository).countResourceByCriteria("reports", 1, null, true);
    }

    @Test
    void unassignedFilterTakesPrecedenceWhenModuleIdIsAlsoProvided() {
        Pageable pageable = new Pageable();
        when(resourceRepository.getResourceByCriteria(null, null, 4, true, pageable)).thenReturn(List.of());
        when(resourceRepository.countResourceByCriteria(null, null, 4, true)).thenReturn(0L);

        service.getListResourceByCriteria(null, null, 4, true, pageable);

        verify(resourceRepository).getResourceByCriteria(null, null, 4, true, pageable);
        verify(resourceRepository).countResourceByCriteria(null, null, 4, true);
    }
}
