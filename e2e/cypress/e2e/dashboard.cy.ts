describe('Dashboard', () => {
  beforeEach(() => {
    cy.visit('/dashboard');
  });

  it('affiche le titre Prix en direct', () => {
    cy.contains('Prix en direct').should('be.visible');
  });

  it('affiche la sidebar avec le lien Dashboard', () => {
    cy.contains('a', 'Dashboard').should('be.visible');
  });

  it('affiche le tableau des prix', () => {
    cy.get('table').should('exist');
    cy.get('thead').contains('Symbole');
    cy.get('thead').contains('Prix');
  });
});
