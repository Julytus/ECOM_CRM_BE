package vn.aeoc.crm.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.aeoc.entity.data.mysql.BoughtProduct;
import vn.aeoc.entity.data.mysql.BoughtService;
import vn.aeoc.entity.data.mysql.Invoice;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceResponse {
    private Invoice invoice;
    private List<BoughtProduct> products;
    private List<BoughtService> services;

}
