"""Public quantity required by a run grid, using the existing CHOICE schema."""
from sls.contracts import PublicEntity

PROJECTION_REVISION = 'sls-public-selection-quantity-v3'


def selection_requirement(count):
    if type(count) is not int or count < 0:
        raise ValueError('invalid public grid selection quantity')
    # Dedicated context entity; amount means required quantity here. No field
    # of a card/relic is repurposed, and no model tensor or vocabulary changes.
    return PublicEntity('selection-requirement','OPTION',(('amount',count),))
