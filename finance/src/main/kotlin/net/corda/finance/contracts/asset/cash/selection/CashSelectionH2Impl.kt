package net.corda.finance.contracts.asset.cash.selection

import net.corda.core.contracts.Amount
import net.corda.core.identity.Party
import net.corda.core.utilities.loggerFor
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.Statement
import java.util.*

class CashSelectionH2Impl : CashSelection() {

    companion object {
        const val JDBC_DRIVER_NAME = "H2 JDBC Driver"
        val log = loggerFor<CashSelectionH2Impl>()
    }

    override fun isCompatible(metadata: DatabaseMetaData): Boolean {
        return metadata.driverName == JDBC_DRIVER_NAME
    }

    override fun toString() = "${this::class.java} for $JDBC_DRIVER_NAME"

    //       We are using an H2 specific means of selecting a minimum set of rows that match a request amount of coins:
    //       1) There is no standard SQL mechanism of calculating a cumulative total on a field and restricting row selection on the
    //          running total of such an accumulator
    //       2) H2 uses session variables to perform this accumulator function:
    //          http://www.h2database.com/html/functions.html#set
    //       3) H2 does not support JOIN's in FOR UPDATE (hence we are forced to execute 2 queries)
    override fun executeQuery(statement: Statement, amount: Amount<Currency>, lockId: UUID, notary: Party?,
                              issuerKeysStr: String?, issuerRefsStr: String?) : ResultSet {
        statement.execute("CALL SET(@t, 0);")

        val selectJoin = """
        SELECT vs.transaction_id, vs.output_index, vs.contract_state, ccs.pennies, SET(@t, ifnull(@t,0)+ccs.pennies) total_pennies, vs.lock_id
        FROM vault_states AS vs, contract_cash_states AS ccs
        WHERE vs.transaction_id = ccs.transaction_id AND vs.output_index = ccs.output_index
        AND vs.state_status = 0
        AND ccs.ccy_code = '${amount.token}' and @t < ${amount.quantity}
        AND (vs.lock_id = '$lockId' OR vs.lock_id is null)
        """ +
                (if (notary != null)
                    " AND vs.notary_name = '${notary.name}'" else "") +
                (if (issuerKeysStr != null)
                    " AND ccs.issuer_key IN ($issuerKeysStr)" else "") +
                (if (issuerRefsStr != null)
                    " AND ccs.issuer_ref IN ($issuerRefsStr)" else "")

        return statement.executeQuery(selectJoin)
    }
}