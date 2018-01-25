import React from 'react';
import { render } from 'react-dom';
import { Button, Grid, Input, Panel } from 'react-bootstrap';
import _ from 'lodash';
import $ from 'jquery';

import Sudoku from './sudoku.js';

require('../styles/style.scss');
require('../styles/dummy.scss');

const dummyStylesheet = document.styleSheets[document.styleSheets.length - 1];
const endpoint = 'solve';

export default class SudokuApp extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      boxesPerRow: 3,
      boxesPerColumn: 3
    };
    _.bindAll(this, 'changeSize', 'handleSubmit');
  }

  componentDidMount() {
    this.updateStylesheet();
  }

  componentWillUpdate(newProps, newState) {
    if (newState.solved != this.state.solved) {
      this.refs.sudoku.setState({
        solution: this.convertJSONToGivens(newState.solution)
      });
    }
  }

  componentDidUpdate(prevProps, prevState) {
    this.updateStylesheet();
  }

  changeSize() {
    if (document.getElementById('boxesPerRow').value && document.getElementById('boxesPerColumn').value) {
      this.setState({
        boxesPerRow: document.getElementById('boxesPerRow').value,
        boxesPerColumn: document.getElementById('boxesPerColumn').value
      });
    }
  }

  clearStylesheet() {
    while (dummyStylesheet.cssRules.length) {
      dummyStylesheet.deleteRule(0);
    }
  }

  updateStylesheet() {
    this.clearStylesheet();

    let containerWidth = $('#sudoku').width();
    let cellWidth = Math.floor(((((containerWidth - 5) / this.state.boxesPerRow) - 4) / this.state.boxesPerColumn) - 2);

    let verticalRule = `.cell:nth-child(${this.state.boxesPerRow}n) { border-right: 5px solid black; }`,
        horizontalRule = `#sudoku :nth-child(${this.state.boxesPerColumn}n) .cell { border-bottom: 5px solid black; }`,
        cellWidthRule = `.cell { width: ${cellWidth}px; }`,
        cellHeightRule = `.cell { height: ${cellWidth}px; }`;

    dummyStylesheet.insertRule(verticalRule, 0);
    dummyStylesheet.insertRule(horizontalRule, 0);
    dummyStylesheet.insertRule(cellWidthRule, 0);
    dummyStylesheet.insertRule(cellHeightRule, 0);

    let actualCellWidth = $('#sudoku .cell').width();
    let fontSizeRule = `.cell { font-size: ${actualCellWidth * 0.9}px; }`,
        lineHeightRule = `.cell { line-height: ${actualCellWidth}px; }`;

    dummyStylesheet.insertRule(fontSizeRule, 0);
    dummyStylesheet.insertRule(lineHeightRule, 0);

    let actualRowWidth = document.querySelector('#row0 .cell').offsetWidth * this.state.boxesPerRow * this.state.boxesPerColumn;
    let rowWidthRule = `#sudoku > div { width: ${actualRowWidth}px; }`;

    dummyStylesheet.insertRule(rowWidthRule, 0);
  }

  /**
   * Technically, converts to an array of JSON objects, so this
   * still needs to be associated with a key to be a legit JSON object.
   */
  convertGivensToJSON() {
    return _.chain(this.refs.sudoku.state.givens).mapValues((value, key) => {
      let s = key.split(',');
      return {
        row: s[0],
        column: s[1],
        digit: value
      }
    }).values().value();
  }

  convertJSONToGivens(array) {
    if (!array) {
      return null;
    }

    return _.reduce(array, (acc, val) => {
      let { row, column, digit } = val;
      acc[`${row},${column}`] = digit;
      return acc;
    }, {});
  }

  handleSubmit() {
    let requestData = {
      givens: this.convertGivensToJSON(),
      boxesPerRow: this.state.boxesPerRow,
      boxesPerColumn: this.state.boxesPerColumn
    };

    $.post(endpoint, JSON.stringify(requestData), 'json').then((data, status, jqXhr) => {
      let solution = JSON.parse(data).solution;
      this.setState({
        solution: solution,
        solved: true
      });
    });
  }

  render() {
    let {
      boxesPerRow,
      boxesPerColumn
    } = this.state;

    return (
      <Panel className="wrapper">
        <Grid id="button-container">
          <Input type="number"
                 id="boxesPerRow"
                 label="boxes per row"
                 min="2"
                 defaultValue="3"
                 bsSize="large"
                 disabled={!!this.state.solution}
                 onChange={this.changeSize} />
          <span>&times;</span>
          <Input type="number"
                 id="boxesPerColumn"
                 label="boxes per column"
                 min="2"
                 defaultValue="3"
                 bsSize="large"
                 disabled={!!this.state.solution}
                 onChange={this.changeSize} />
          <Button id="solve"
                  bsStyle="primary"
                  bsSize="large"
                  onClick={this.handleSubmit}
                  disabled={!!this.state.solution}>
            Solve
          </Button>
        </Grid>
        <Sudoku size={boxesPerRow * boxesPerColumn} ref='sudoku' />
      </Panel>
    );
  }
}

render(<SudokuApp />, document.getElementById('app'));
