package vn.aeoc.crm.data.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.aeoc.entity.data.mysql.sale.BoughtProduct;
import vn.aeoc.entity.data.mysql.sale.BoughtService;
import vn.aeoc.entity.data.mysql.sale.Invoice;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceResponse {
    private Invoice invoice;
    private List<BoughtProduct> products;
    private List<BoughtService> services;

}
