package com.priceradar.ingestion;

import java.util.HashMap;
import java.util.LinkedHashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.priceradar.ingestion.ImportResult.Status;

/**
 * Writes one validated search file to the marketplace tables in a single
 * transaction: either the whole file is imported or nothing is.
 *
 * <p>A product keeps the catalogue values it was first imported with; every
 * file adds its own observations, one per result row.
 */
@Service
public class SearchFileImporter {

	/** One file has at most a few dozen rows; anything slower is stuck, usually on a lock. */
	private static final int TIMEOUT_SECONDS = 60;

	private final MarketplaceImportRepository repository;
	private final TransactionTemplate writeTransaction;
	private final TransactionTemplate readTransaction;

	public SearchFileImporter(MarketplaceImportRepository repository, PlatformTransactionManager transactionManager) {
		this.repository = repository;
		this.writeTransaction = new TransactionTemplate(transactionManager);
		this.writeTransaction.setTimeout(TIMEOUT_SECONDS);
		this.readTransaction = new TransactionTemplate(transactionManager);
		this.readTransaction.setTimeout(TIMEOUT_SECONDS);
		this.readTransaction.setReadOnly(true);
	}

	/** Writes the file. Importing the same bytes again changes nothing. */
	public ImportResult importFile(SearchFile file) {
		return writeTransaction.execute(status -> {
			repository.lockForImport();
			if (wasImported(file)) {
				return result(file, Status.ALREADY_IMPORTED, 0);
			}

			var storeId = repository.ensureStore(file.marketplace());
			var sourceId = repository.insertSource(file, storeId);
			var productIds = new HashMap<>(repository.findProductIds(storeId, externalIds(file)));
			var newProducts = 0;

			for (var rowIndex = 0; rowIndex < file.items().size(); rowIndex++) {
				var item = file.items().get(rowIndex);
				var productId = productIds.get(item.externalId());
				if (productId == null) {
					productId = repository.insertProduct(storeId, item.externalId(), item.listing());
					productIds.put(item.externalId(), productId);
					newProducts++;
				}
				repository.insertObservation(storeId, productId, sourceId, rowIndex, file.fetchedAt(), item);
			}
			return result(file, Status.IMPORTED, newProducts);
		});
	}

	/** Reports what {@link #importFile} would do, without writing anything. */
	public ImportResult checkFile(SearchFile file) {
		return readTransaction.execute(status -> {
			if (wasImported(file)) {
				return result(file, Status.ALREADY_IMPORTED, 0);
			}
			var externalIds = externalIds(file);
			var known = repository.findStoreId(file.marketplace())
					.map(storeId -> repository.findProductIds(storeId, externalIds).size())
					.orElse(0);
			return result(file, Status.WOULD_IMPORT, externalIds.size() - known);
		});
	}

	/**
	 * True when these exact bytes were imported before, at this or another path.
	 *
	 * @throws SourceConflictException when this path was imported with different bytes
	 */
	private boolean wasImported(SearchFile file) {
		var recorded = repository.findSources(file.path(), file.sha256());
		for (var source : recorded) {
			if (!source.sha256().equals(file.sha256())) {
				throw new SourceConflictException(file.path());
			}
		}
		return !recorded.isEmpty();
	}

	private static LinkedHashSet<String> externalIds(SearchFile file) {
		var ids = new LinkedHashSet<String>();
		file.items().forEach(item -> ids.add(item.externalId()));
		return ids;
	}

	private static ImportResult result(SearchFile file, Status status, int newProducts) {
		return new ImportResult(file.path(), status, file.items().size(), newProducts);
	}

}
